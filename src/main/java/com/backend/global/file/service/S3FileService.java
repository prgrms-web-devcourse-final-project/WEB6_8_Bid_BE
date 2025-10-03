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

    @Value("${file.upload.s3.bucket}")
    private String bucketName;

    @Value("${file.upload.s3.base-url}")
    private String baseUrl;

    @Override
    public String uploadFile(MultipartFile file, String directory) {
        try {
            // 고유한 파일명 생성
            String key = directory + "/" + UUID.randomUUID() + getFileExtension(file.getOriginalFilename());

            // S3 키 생성
            Map<String, String> metadata = new HashMap<>();
            metadata.put("original-filename", file.getOriginalFilename());

            // S3 업로드
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .metadata(metadata)
                    .acl(ObjectCannedACL.PUBLIC_READ) // 공개 읽기 권한
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String fileUrl = baseUrl + "/" + key;
            log.info("S3 파일 업로드 성공: {}", fileUrl);

            return fileUrl;
        } catch (Exception e) {
            log.error("S3 파일 업로드 실패: {}", file.getOriginalFilename(), e);
            throw ProductException.fileUploadFailed();
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

            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.info("S3 파일 삭제 성공: {}", fileUrl);
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", fileUrl, e);
        }
    }
}