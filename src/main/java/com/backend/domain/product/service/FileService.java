package com.backend.domain.product.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface FileService {
    String uploadFile(MultipartFile file, String directory);
    void deleteFile(String fileUrl);
}
