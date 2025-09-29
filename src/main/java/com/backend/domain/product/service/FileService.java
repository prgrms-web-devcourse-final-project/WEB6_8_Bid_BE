package com.backend.domain.product.service;

import com.backend.domain.product.exception.ProductException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileService {
    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.upload.base-url}")
    private String baseUrl;

    public String uploadFile(MultipartFile file, String directory) {
        try {
            // 디렉토리 생성
            String fullPath = uploadPath + "/" + directory;
            Path dir = Paths.get(fullPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            // 고유한 파일명 생성
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID() + extension;

            // 파일 저장
            Path targetPath = Paths.get(fullPath, uniqueFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 반환할 URL 생성
            String fileUrl = baseUrl + "/" + directory + "/" + uniqueFilename;
            log.info("로컬 파일 업로드 성공: {}", fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("로컬 파일 업로드 실패: {}", file.getOriginalFilename(), e);
            throw ProductException.fileUploadFailed();
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    public void deleteFile(String fileUrl) {
        try {
            if (!fileUrl.startsWith(baseUrl)) {
                log.warn("잘못된 파일 URL: {}", fileUrl);
                return;
            }

            String filePath = fileUrl.replace(baseUrl, uploadPath);
            Path path = Paths.get(filePath);

            // 보안: uploadPath 외부 파일 접근 방지
            if (!path.normalize().startsWith(Paths.get(uploadPath).normalize())) {
                log.warn("보안 위반: uploadPath 외부 접근 시도 - {}", fileUrl);
                return;
            }

            Files.deleteIfExists(path);
            log.info("로컬 파일 삭제 성공: {}", fileUrl);
        } catch (Exception e) {
            log.error("로컬 파일 삭제 실패: {}", fileUrl, e);
        }
    }
}