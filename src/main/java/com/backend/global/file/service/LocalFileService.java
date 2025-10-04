package com.backend.global.file.service;

import com.backend.domain.product.exception.ProductException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 로컬 파일 시스템을 사용하는 파일 서비스 구현체
 * - 개발 환경(!prod)에서 활성화
 * - 서버의 로컬 디렉토리에 파일 저장
 * - 접근 URL은 정적 리소스 경로를 통해 제공
 */
@Slf4j
@Service
@Profile("!prod")
public class LocalFileService implements FileService {
    @Value("${file.upload.path}")
    private String uploadPath;  // 파일 저장 경로 - /uploads

    @Value("${file.upload.base-url}")
    private String baseUrl;  // 파일 접근 URL - http://localhost:8080/uploads

    /**
     * 로컬 파일 시스템에 파일 업로드
     *
     * @param file 업로드할 파일
     * @param directory 저장할 디렉토리 (예: "products/1")
     * @return 파일 접근 URL (예: http://localhost:8080/uploads/products/1/uuid.jpg)
     * @throws ProductException 파일 업로드 실패 시
     */
    public String uploadFile(MultipartFile file, String directory) {
        try {
            // 1. 전체 경로 생성 및 디렉토리 생성
            String fullPath = uploadPath + "/" + directory;
            Path dir = Paths.get(fullPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            // 2. 고유한 파일명 생성 (UUID + 확장자)
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID() + extension;

            // 3. 파일 저장
            Path targetPath = Paths.get(fullPath, uniqueFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 4. 반환할 URL 생성
            String fileUrl = baseUrl + "/" + directory + "/" + uniqueFilename;
            log.info("로컬 파일 업로드 성공: {}", fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("로컬 파일 업로드 실패: {}", file.getOriginalFilename(), e);
            throw ProductException.fileUploadFailed();
        }
    }

    /**
     * 로컬 파일 시스템에서 파일 삭제
     *
     * @param fileUrl 삭제할 파일의 URL
     */
    public void deleteFile(String fileUrl) {
        try {
            // URL 형식 검증
            if (!fileUrl.startsWith(baseUrl)) {
                log.warn("잘못된 파일 URL: {}", fileUrl);
                return;
            }

            // URL을 파일 시스템 경로로 변환
            String filePath = fileUrl.replace(baseUrl, uploadPath);
            Path path = Paths.get(filePath);

            // 보안: uploadPath 외부 파일 접근 방지 (경로 탐색 공격 차단)
            if (!path.normalize().startsWith(Paths.get(uploadPath).normalize())) {
                log.warn("보안 위반: uploadPath 외부 접근 시도 - {}", fileUrl);
                return;
            }

            // 파일 삭제 (파일이 없어도 예외 발생하지 않음)
            Files.deleteIfExists(path);
            log.info("로컬 파일 삭제 성공: {}", fileUrl);
        } catch (Exception e) {
            log.error("로컬 파일 삭제 실패: {}", fileUrl, e);
            // 삭제 실패는 치명적이지 않으므로 예외를 전파하지 않음
        }
    }
}