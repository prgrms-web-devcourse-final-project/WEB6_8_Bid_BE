package com.backend.global.file.service;

import com.backend.domain.product.exception.ProductException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3FileService 테스트")
class S3FileServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3FileService s3FileService;

    private static final String BUCKET_NAME = "test-bucket";
    private static final String BASE_URL = "https://test-bucket.s3.amazonaws.com";

    @BeforeEach
    void setUp() {
        // @Value 필드 값 주입
        ReflectionTestUtils.setField(s3FileService, "bucketName", BUCKET_NAME);
        ReflectionTestUtils.setField(s3FileService, "baseUrl", BASE_URL);
    }

    @Test
    @DisplayName("파일 업로드 성공")
    void uploadFile_Success() {
        // given
        String originalFilename = "test-image.jpg";
        String contentType = "image/jpeg";
        byte[] content = "test content".getBytes();
        MultipartFile file = new MockMultipartFile(
                "file",
                originalFilename,
                contentType,
                content
        );
        String directory = "products";

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(null);

        // when
        String result = s3FileService.uploadFile(file, directory);

        // then
        assertThat(result).startsWith(BASE_URL + "/" + directory + "/");
        assertThat(result).endsWith(".jpg");
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - S3 에러")
    void uploadFile_Fail_S3Error() {
        // given
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "content".getBytes()
        );
        String directory = "products";

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 Error"));

        // when & then
        assertThatThrownBy(() -> s3FileService.uploadFile(file, directory))
                .isInstanceOf(ProductException.class);
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - IOException")
    void uploadFile_Fail_IOException() throws IOException {
        // given
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getInputStream()).thenThrow(new IOException("IO Error"));
        String directory = "products";

        // when & then
        assertThatThrownBy(() -> s3FileService.uploadFile(file, directory))
                .isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("파일 확장자 추출 - 일반 파일")
    void uploadFile_ExtractExtension() {
        // given
        MultipartFile file = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                "content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(null);

        // when
        String result = s3FileService.uploadFile(file, "test");

        // then
        assertThat(result).endsWith(".png");
    }

    @Test
    @DisplayName("파일 확장자 추출 - 확장자 없는 파일")
    void uploadFile_NoExtension() {
        // given
        MultipartFile file = new MockMultipartFile(
                "file",
                "image",
                "image/png",
                "content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(null);

        // when
        String result = s3FileService.uploadFile(file, "test");

        // then
        assertThat(result).contains("test/");
    }

    @Test
    @DisplayName("파일 삭제 성공")
    void deleteFile_Success() {
        // given
        String fileUrl = BASE_URL + "/products/test-uuid.jpg";
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(null);

        // when
        s3FileService.deleteFile(fileUrl);

        // then
        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("파일 삭제 실패 - S3 에러 발생해도 예외 던지지 않음")
    void deleteFile_Fail_NoException() {
        // given
        String fileUrl = BASE_URL + "/products/test-uuid.jpg";
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(new RuntimeException("S3 Delete Error"));

        // when & then
        // 예외가 발생하지 않아야 함 (로그만 기록)
        s3FileService.deleteFile(fileUrl);
        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 - PutObjectRequest 검증")
    void uploadFile_VerifyPutObjectRequest() {
        // given
        String originalFilename = "test.jpg";
        String contentType = "image/jpeg";
        byte[] content = "test".getBytes();
        MultipartFile file = new MockMultipartFile(
                "file",
                originalFilename,
                contentType,
                content
        );
        String directory = "products";

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        // when
        s3FileService.uploadFile(file, directory);

        // then
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo(BUCKET_NAME);
        assertThat(capturedRequest.key()).startsWith(directory + "/");
        assertThat(capturedRequest.contentType()).isEqualTo(contentType);
        assertThat(capturedRequest.contentLength()).isEqualTo((long) content.length);
        assertThat(capturedRequest.metadata()).containsEntry("original-filename", originalFilename);
    }

    @Test
    @DisplayName("파일 삭제 - DeleteObjectRequest 검증")
    void deleteFile_VerifyDeleteObjectRequest() {
        // given
        String key = "products/test-uuid.jpg";
        String fileUrl = BASE_URL + "/" + key;

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);

        // when
        s3FileService.deleteFile(fileUrl);

        // then
        verify(s3Client).deleteObject(requestCaptor.capture());

        DeleteObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo(BUCKET_NAME);
        assertThat(capturedRequest.key()).isEqualTo(key);
    }
}