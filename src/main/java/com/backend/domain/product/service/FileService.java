package com.backend.domain.product.service;

import com.backend.global.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileService {
    private final String uploadPath = "./uploads";

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
            return "/uploads/" + directory + "/" + uniqueFilename;
        } catch (IOException e) {
            throw new ServiceException("400-4", "이미지 파일 업로드에 실패했습니다.");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    public boolean deleteFile(String fileUrl) {
        try {
            // "/uploads/products/1/uuid.jpg" -> "./uploads/products/1/uuid.jpg"
            String filePath = fileUrl.replace("/uploads/", uploadPath + "/");
            Path path = Paths.get(filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }
}
