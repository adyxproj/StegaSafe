package com.stegasafe.stego;

import com.stegasafe.crypto.CryptoEngine;
import com.stegasafe.dto.CapacityInfo;
import com.stegasafe.dto.DecodeResult;
import com.stegasafe.exception.CapacityExceededException;
import com.stegasafe.exception.InvalidPasswordException;
import com.stegasafe.exception.NoHiddenMessageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LsbSteganographyServiceTest {

    private LsbSteganographyService service;

    @BeforeEach
    void setUp() {
        CryptoEngine cryptoEngine = new CryptoEngine();
        service = new LsbSteganographyService(cryptoEngine);
    }

    private MockMultipartFile createTestImage(int width, int height, String format, String filename) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(45, 85, 125));
        g.fillRect(0, 0, width, height);
        g.setColor(Color.WHITE);
        g.drawString("StegaSafe Test Image", 10, height / 2);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        byte[] bytes = baos.toByteArray();

        String contentType = "image/" + format.toLowerCase();
        return new MockMultipartFile("image", filename, contentType, bytes);
    }

    @Test
    @DisplayName("Analyze capacity correctly computes dimensions and byte capacity")
    void testCapacityAnalysis() throws IOException {
        MockMultipartFile file = createTestImage(100, 100, "png", "test.png");
        CapacityInfo info = service.analyzeCapacity(file);

        assertThat(info.getWidth()).isEqualTo(100);
        assertThat(info.getHeight()).isEqualTo(100);
        assertThat(info.getTotalPixels()).isEqualTo(10000L);
        // 100 * 100 * 3 / 8 = 3750 bytes
        assertThat(info.getRawCapacityBytes()).isEqualTo(3750L);
        assertThat(info.getMaxMessageBytesUnencrypted()).isEqualTo(3750L - LsbSteganographyService.UNENCRYPTED_HEADER_OVERHEAD);
        assertThat(info.getMaxMessageBytesEncrypted()).isEqualTo(3750L - LsbSteganographyService.ENCRYPTED_HEADER_OVERHEAD);
    }

    @Test
    @DisplayName("Encode and decode unencrypted message cleanly")
    void testEncodeAndDecodeUnencrypted() throws IOException {
        MockMultipartFile sourceFile = createTestImage(150, 150, "png", "carrier.png");
        String secretMessage = "Hello World! This is an unencrypted secret payload.";

        byte[] encodedPngBytes = service.encodeMessage(sourceFile, secretMessage, null);
        assertThat(encodedPngBytes).isNotEmpty();

        MockMultipartFile encodedFile = new MockMultipartFile("image", "carrier_encoded.png", "image/png", encodedPngBytes);
        DecodeResult result = service.decodeMessage(encodedFile, null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isEncrypted()).isFalse();
        assertThat(result.getMessage()).isEqualTo(secretMessage);
    }

    @Test
    @DisplayName("Encode and decode AES-256-GCM encrypted message with password")
    void testEncodeAndDecodeEncrypted() throws IOException {
        MockMultipartFile sourceFile = createTestImage(150, 150, "png", "carrier.png");
        String secretMessage = "Confidential Credentials: admin/SuperSecretKey#2026";
        String password = "CorrectVaultPassword!99";

        byte[] encodedPngBytes = service.encodeMessage(sourceFile, secretMessage, password);
        MockMultipartFile encodedFile = new MockMultipartFile("image", "encoded.png", "image/png", encodedPngBytes);

        // Attempt decode with correct password
        DecodeResult result = service.decodeMessage(encodedFile, password);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isEncrypted()).isTrue();
        assertThat(result.getMessage()).isEqualTo(secretMessage);

        // Attempt decode with wrong password
        assertThatThrownBy(() -> service.decodeMessage(encodedFile, "WrongPassword"))
                .isInstanceOf(InvalidPasswordException.class);

        // Attempt decode without providing required password
        assertThatThrownBy(() -> service.decodeMessage(encodedFile, null))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("password-protected");
    }

    @Test
    @DisplayName("Encode and decode multi-language Unicode text including Tamil and emojis")
    void testEncodeAndDecodeTamilAndUnicode() throws IOException {
        MockMultipartFile sourceFile = createTestImage(200, 200, "png", "carrier.png");
        String secretMessage = "வணக்கம் நண்பா! இது ஒரு பாதுகாப்பான ஸ்டெகனோகிராபி செய்தி. 🔐🛡️🚀";
        String password = "தமிழ்_கடவுச்சொல்";

        byte[] encodedPngBytes = service.encodeMessage(sourceFile, secretMessage, password);
        MockMultipartFile encodedFile = new MockMultipartFile("image", "encoded.png", "image/png", encodedPngBytes);

        DecodeResult result = service.decodeMessage(encodedFile, password);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo(secretMessage);
    }

    @Test
    @DisplayName("Uploading JPEG image must convert and produce decodable PNG")
    void testJpegSourceEncodingToPng() throws IOException {
        MockMultipartFile jpgFile = createTestImage(120, 120, "jpg", "photo.jpg");
        String secret = "Secret message hidden inside a converted JPEG image.";

        byte[] pngResultBytes = service.encodeMessage(jpgFile, secret, null);

        // Verify output is readable PNG
        BufferedImage decodedImg = ImageIO.read(new java.io.ByteArrayInputStream(pngResultBytes));
        assertThat(decodedImg).isNotNull();

        MockMultipartFile encodedFile = new MockMultipartFile("image", "photo_encoded.png", "image/png", pngResultBytes);
        DecodeResult decodeResult = service.decodeMessage(encodedFile, null);
        assertThat(decodeResult.getMessage()).isEqualTo(secret);
    }

    @Test
    @DisplayName("Decoding image without StegaSafe payload throws NoHiddenMessageException")
    void testDecodeUnmodifiedImageThrows() throws IOException {
        MockMultipartFile plainImage = createTestImage(100, 100, "png", "plain.png");

        assertThatThrownBy(() -> service.decodeMessage(plainImage, null))
                .isInstanceOf(NoHiddenMessageException.class)
                .hasMessageContaining("No StegaSafe hidden message detected");
    }

    @Test
    @DisplayName("Message exceeding capacity throws CapacityExceededException")
    void testCapacityExceeded() throws IOException {
        // Small image 10x10 = 100 pixels = 300 bits = 37 bytes total raw capacity
        MockMultipartFile tinyImage = createTestImage(10, 10, "png", "tiny.png");
        String giantMessage = "This message is far too long to fit into a tiny 10x10 pixel image because it exceeds 37 bytes!";

        assertThatThrownBy(() -> service.encodeMessage(tinyImage, giantMessage, null))
                .isInstanceOf(CapacityExceededException.class)
                .hasMessageContaining("exceeds image capacity");
    }
}
