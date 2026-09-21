package uz.sevenEdu.teacherBot.common.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;
import uz.sevenEdu.teacherBot.common.exception.PayloadTooLargeException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void validPngIsStoredWithServerGeneratedName() {
        FileStorageService service = new FileStorageService(tempDir.toString());

        StepVerifier.create(service.saveLandingImage(file("photo.png", MediaType.IMAGE_PNG, pngBytes())))
                .assertNext(path -> {
                    assertThat(path).matches("landing/landing_[0-9a-f]{12}\\.png");
                    assertThat(tempDir.resolve(path)).exists().isRegularFile();
                })
                .verifyComplete();
    }

    @Test
    void spoofedMimeIsRejectedWithoutWritingAFile() throws Exception {
        FileStorageService service = new FileStorageService(tempDir.toString());

        StepVerifier.create(service.saveLandingImage(file("photo.png", MediaType.IMAGE_JPEG, pngBytes())))
                .expectError(BadRequestException.class)
                .verify();

        assertLandingDirectoryEmpty();
    }

    @Test
    void spoofedMagicIsRejected() {
        FileStorageService service = new FileStorageService(tempDir.toString());
        byte[] html = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);

        StepVerifier.create(service.saveLandingImage(file("photo.png", MediaType.IMAGE_PNG, html)))
                .expectError(BadRequestException.class)
                .verify();
    }

    @Test
    void mismatchedExtensionIsRejected() {
        FileStorageService service = new FileStorageService(tempDir.toString());

        StepVerifier.create(service.saveLandingImage(file("photo.jpg", MediaType.IMAGE_PNG, pngBytes())))
                .expectError(BadRequestException.class)
                .verify();
    }

    @Test
    void oversizedImageIsRejected() {
        FileStorageService service = new FileStorageService(tempDir.toString());
        byte[] oversized = new byte[FileStorageService.MAX_LANDING_IMAGE_BYTES + 1];

        StepVerifier.create(service.saveLandingImage(file("photo.png", MediaType.IMAGE_PNG, oversized)))
                .expectError(PayloadTooLargeException.class)
                .verify();
    }

    private FilePart file(String filename, MediaType mediaType, byte[] bytes) {
        FilePart part = mock(FilePart.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        when(part.filename()).thenReturn(filename);
        when(part.headers()).thenReturn(headers);
        when(part.content()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes)));
        return part;
    }

    private byte[] pngBytes() {
        byte[] bytes = new byte[45];
        copy(bytes, 0, new int[]{0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
        copyAscii(bytes, 12, "IHDR");
        copy(bytes, 33, new int[]{0, 0, 0, 0, 0x49, 0x45, 0x4e, 0x44, 0xae, 0x42, 0x60, 0x82});
        return bytes;
    }

    private void copy(byte[] target, int offset, int[] source) {
        for (int i = 0; i < source.length; i++) target[offset + i] = (byte) source[i];
    }

    private void copyAscii(byte[] target, int offset, String source) {
        byte[] bytes = source.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, target, offset, bytes.length);
    }

    private void assertLandingDirectoryEmpty() throws Exception {
        Path landing = tempDir.resolve("landing");
        if (!Files.exists(landing)) return;
        try (var files = Files.list(landing)) {
            assertThat(files).isEmpty();
        }
    }
}
