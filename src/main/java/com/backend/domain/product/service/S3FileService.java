package com.backend.domain.product.service;

import com.backend.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class S3FileService implements FileService {
    private final S3Client s3Client;

    @Value("${file.upload.bucket}")
    private String bucketName;

    @Value("${file.upload.base-url}")
    private String baseUrl;

    @Override
    public String uploadFile(MultipartFile file, String directory) {
        try {
            // 고유한 파일명 생성
            String key = directory + "/" + UUID.randomUUID() + getFileExtension(file.getOriginalFilename());

            // S3 키 생성
            Map<String, String> metadata = new HashMap<>();
            metadata.put("Content-Type", file.getContentType());
            metadata.put("Content-Length", String.valueOf(file.getSize()));
            metadata.put("original-filename", file.getOriginalFilename());

            // S3 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .metadata(metadata)
                    .acl(ObjectCannedACL.PUBLIC_READ) // 공개 읽기 권한
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("S3 파일 업로드 성공: bucket={}, key={}", bucketName, key);

            // URL 반환
            return baseUrl + "/" + key;
        } catch (Exception e) {
            log.error("S3 파일 업로드 실패: {}", file.getOriginalFilename(), e);
            throw new ServiceException("400-4", "이미지 파일 업로드에 실패했습니다.");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            // URL에서 S3 키 추출
            String key = fileUrl.replace(baseUrl + "/", "");

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 파일 삭제 성공: bucket={}, key={}", bucketName, key);
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", fileUrl, e);
            throw new ServiceException("400-5", "이미지 파일 삭제에 실패했습니다.");
        }
    }
}
