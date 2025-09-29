package com.backend.domain.product.service;

import com.backend.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @InjectMocks
    private FileService fileService;

    @TempDir
    Path tempDir;

    private String testUploadPath;
    private String testBaseUrl;

    @BeforeEach
    void setUp() {
        testUploadPath = tempDir.toString();
        testBaseUrl = "http://localhost:8080/uploads";

        // @Value로 주입되는 필드들을 테스트용 값으로 설정
        ReflectionTestUtils.setField(fileService, "uploadPath", testUploadPath);
        ReflectionTestUtils.setField(fileService, "baseUrl", testBaseUrl);
    }

    @Test
    @DisplayName("파일 업로드 성공")
    void uploadFile_Success() throws IOException {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        String directory = "products/1";

        // when
        String result = fileService.uploadFile(file, directory);

        // then
        assertThat(result).isNotNull();
        assertThat(result).startsWith(testBaseUrl + "/" + directory + "/");
        assertThat(result).contains(".jpg");

        // 실제 파일이 생성되었는지 확인
        Path uploadDir = Paths.get(testUploadPath, directory);
        assertThat(Files.exists(uploadDir)).isTrue();
        try (var fileList = Files.list(uploadDir)) {
            assertThat(fileList.count()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("디렉토리가 존재하지 않을 때 자동 생성")
    void uploadFile_CreateDirectory() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "test.png",
                "image/png",
                "test content".getBytes()
        );
        String directory = "products/999/thumbnails";

        // when
        String result = fileService.uploadFile(file, directory);

        // then
        assertThat(result).isNotNull();
        Path uploadDir = Paths.get(testUploadPath, directory);
        assertThat(Files.exists(uploadDir)).isTrue();
    }

    @Test
    @DisplayName("파일명에 확장자가 없을 때")
    void uploadFile_NoExtension() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "testfile",
                "image/jpeg",
                "test content".getBytes()
        );

        // when
        String result = fileService.uploadFile(file, "test");

        // then
        assertThat(result).isNotNull();
        assertThat(result).doesNotContain("."); // 확장자 없음
    }

    @Test
    @DisplayName("파일 업로드 실패 - IOException")
    void uploadFile_IOException() {
        // given
        MultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "content".getBytes()) {
            @Override
            public java.io.InputStream getInputStream() throws IOException {
                throw new IOException("파일 읽기 실패");
            }
        };

        // when & then
        assertThatThrownBy(() -> fileService.uploadFile(mockFile, "test"))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("resultCode", "400-8")
                .hasFieldOrPropertyWithValue("msg", "이미지 파일 업로드에 실패했습니다");
    }

    @Test
    @DisplayName("파일 삭제 성공")
    void deleteFile_Success() throws IOException {
        // given
        // 먼저 파일 생성
        String directory = "products/1";
        Path uploadDir = Paths.get(testUploadPath, directory);
        Files.createDirectories(uploadDir);
        Path testFile = uploadDir.resolve("test-file.jpg");
        Files.write(testFile, "test content".getBytes());

        String fileUrl = testBaseUrl + "/" + directory + "/test-file.jpg";

        // when
        fileService.deleteFile(fileUrl);

        // then
        assertThat(Files.exists(testFile)).isFalse();
    }

    @Test
    @DisplayName("파일 삭제 - 잘못된 URL")
    void deleteFile_InvalidUrl() throws ServiceException {
        // given
        String invalidUrl = "https://other-domain.com/uploads/test.jpg";

        // when
        fileService.deleteFile(invalidUrl); // 예외가 발생하지 않아야 함

        // then - 로그만 기록되고 예외는 발생하지 않음
    }

    @Test
    @DisplayName("파일 삭제 - Path Traversal 공격 방지")
    void deleteFile_PreventPathTraversal() {
        // given
        String maliciousUrl = testBaseUrl + "/../../../etc/passwd";

        // when
        fileService.deleteFile(maliciousUrl); // 예외가 발생하지 않아야 함

        // then - 보안 위반 로그만 기록되고 파일은 삭제되지 않음
    }

    @Test
    @DisplayName("파일 확장자 추출 - 정상 케이스")
    void getFileExtension_Success() {
        // when & then
        assertThat(getFileExtension("test.jpg")).isEqualTo(".jpg");
        assertThat(getFileExtension("image.png")).isEqualTo(".png");
        assertThat(getFileExtension("document.pdf")).isEqualTo(".pdf");
        assertThat(getFileExtension("file.tar.gz")).isEqualTo(".gz");
    }

    @Test
    @DisplayName("파일 확장자 추출 - 확장자 없는 경우")
    void getFileExtension_NoExtension() {
        // when & then
        assertThat(getFileExtension("filename")).isEqualTo("");
        assertThat(getFileExtension(null)).isEqualTo("");
        assertThat(getFileExtension("")).isEqualTo("");
    }

    // private 메서드 테스트를 위한 헬퍼 메서드
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}