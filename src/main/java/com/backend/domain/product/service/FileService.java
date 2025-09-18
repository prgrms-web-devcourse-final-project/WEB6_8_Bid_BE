package com.backend.domain.product.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    String uploadFile(MultipartFile file, String directory);
    void deleteFile(String fileUrl);
}
