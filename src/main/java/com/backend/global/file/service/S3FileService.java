package com.backend.global.file.service;

import com.backend.domain.product.exception.ProductException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AWS S3를 사용하는 파일 서비스 구현체
 * - 운영 환경(prod)에서 활성화
 * - S3 버킷에 파일 저장
 * - S3 Public URL을 통해 파일 직접 제공
 *
 * 인프라 구성 (Terraform):
 * - 인증 방식: EC2 인스턴스에 IAM 역할 부여 (AmazonS3FullAccess)
 * - 공개 접근: S3 버킷 공개 접근 허용 (block_public_acls: false)
 * - 버킷 정책: 공개 읽기 권한 부여 (s3:GetObject)
 * - CORS 설정 (GET, HEAD 메서드 허용)
 */
@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class S3FileService implements FileService {
    private final S3Client s3Client;    // AWS SDK v2 S3 클라이언트

    @Value("${file.upload.s3.bucket}")
    private String bucketName;  // S3 버킷 이름 - team12-bid-market-bucket

    @Value("${file.upload.s3.base-url}")
    private String baseUrl;     // 파일 접근 URL - https://team12-bid-market-bucket.s3.ap-northeast-2.amazonaws.com

    /**
     * AWS S3에 파일 업로드
     *
     * 업로드 설정:
     * - contentType: 브라우저에서 파일을 올바르게 표시하기 위해 MIME 타입 설정
     * - contentLength: 파일 크기 명시 (필수)
     * - metadata: 원본 파일명 보존 (추후 다운로드 시 참조 가능)
     *
     * @param file 업로드할 파일
     * @param directory S3 키의 prefix (예: "products/1")
     * @return 파일 접근 URL (예: https://team12-bid-market-bucket.s3.ap-northeast-2.amazonaws.com/products/1/uuid.jpg)
     * @throws ProductException S3 업로드 실패 시
     */
    @Override
    public String uploadFile(MultipartFile file, String directory) {
        try {
            // 1. S3 키 생성 (고유 파일명 - directory/uuid.확장자)
            String key = directory + "/" + UUID.randomUUID() + getFileExtension(file.getOriginalFilename());

            // 2. 메타데이터 설정 (원본 파일명 보존)
            Map<String, String> metadata = new HashMap<>();
            metadata.put("original-filename", file.getOriginalFilename());

            // 3. S3 업로드 요청 생성
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType()) // MIME 타입 설정 (예: image/jpeg)
                    .contentLength(file.getSize())      // 파일 크기 설정
                    .metadata(metadata)                 // 커스텀 메타데이터 (원본 파일명)
//                    .acl(ObjectCannedACL.PUBLIC_READ) // 공개 읽기 권한 -> 버킷 정책으로 처리 (Terraform)
                    .build();

            // 4. S3에 파일 업로드 (EC2 IAM 역할의 자격 증명 자동 사용)
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // 5. 공개 접근 가능한 S3 URL 생성 및 반환
            String fileUrl = baseUrl + "/" + key;
            log.info("S3 파일 업로드 성공: {}", fileUrl);

            return fileUrl;
        } catch (Exception e) {
            log.error("S3 파일 업로드 실패: {}", file.getOriginalFilename(), e);
            throw ProductException.fileUploadFailed();
        }
    }

    /**
     * AWS S3에서 파일 삭제
     *
     * 주의사항:
     * - 존재하지 않는 파일 삭제 시도는 무시됨 (예외 발생 안 함)
     * - 버킷 버전 관리가 비활성화되어 있어 완전히 삭제됨
     * - 삭제 권한은 EC2 IAM 역할을 통해 부여됨
     *
     * @param fileUrl 삭제할 파일의 S3 URL
     */
    @Override
    public void deleteFile(String fileUrl) {
        try {
            // 1. URL에서 S3 키 추출
            // 예: https://team12-bid-market-bucket.s3.ap-northeast-2.amazonaws.com/products/1/uuid.jpg -> products/1/uuid.jpg
            String key = fileUrl.replace(baseUrl + "/", "");

            // 2. S3 삭제 요청 생성 및 실행
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.info("S3 파일 삭제 성공: {}", fileUrl);
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", fileUrl, e);
            // 삭제 실패는 치명적이지 않으므로 예외를 전파하지 않음
        }
    }
}