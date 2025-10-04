package com.backend.global.file.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 업로드/삭제를 위한 추상 인터페이스
 * - 구현체: LocalFileService (개발 환경), S3FileService (운영 환경)
 * - 프로파일에 따라 자동으로 구현체 선택됨
 */
public interface FileService {
    /**
     * 파일 업로드
     * @param file 업로드할 파일
     * @param directory 저장 디렉토리 (예: "products/123")
     * @return 업로드된 파일의 접근 URL
     */
    String uploadFile(MultipartFile file, String directory);

    /**
     * 파일 삭제
     * @param fileUrl 삭제할 파일의 URL
     */
    void deleteFile(String fileUrl);

    /**
     * 파일명에서 확장자 추출
     * @param filename 파일명
     * @return 확장자 (점 포함, 예: ".jpg")
     */
    default String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}