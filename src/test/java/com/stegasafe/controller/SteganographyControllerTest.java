package com.stegasafe.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SteganographyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private MockMultipartFile createTestPng(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, new Color(50, 100, 150).getRGB());
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return new MockMultipartFile("image", "sample.png", "image/png", baos.toByteArray());
    }

    @Test
    @DisplayName("POST /api/steganography/capacity returns capacity metadata")
    void testCapacityEndpoint() throws Exception {
        MockMultipartFile file = createTestPng(120, 80);

        mockMvc.perform(multipart("/api/steganography/capacity").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.width").value(120))
                .andExpect(jsonPath("$.data.height").value(80))
                .andExpect(jsonPath("$.data.totalPixels").value(9600));
    }

    @Test
    @DisplayName("POST /api/steganography/encode and decode full lifecycle")
    void testEncodeAndDecodeLifecycle() throws Exception {
        MockMultipartFile originalFile = createTestPng(100, 100);
        String secret = "Cybersecurity Project Verification 2026";
        String password = "LockKey@9988";

        // Step 1: Encode
        byte[] encodedBytes = mockMvc.perform(
                        multipart("/api/steganography/encode")
                                .file(originalFile)
                                .param("message", secret)
                                .param("password", password)
                )
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // Step 2: Decode with correct password
        MockMultipartFile encodedFile = new MockMultipartFile("image", "sample_encoded.png", "image/png", encodedBytes);

        mockMvc.perform(
                        multipart("/api/steganography/decode")
                                .file(encodedFile)
                                .param("password", password)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value(secret))
                .andExpect(jsonPath("$.data.encrypted").value(true));

        // Step 3: Decode with wrong password -> 401 Unauthorized
        mockMvc.perform(
                        multipart("/api/steganography/decode")
                                .file(encodedFile)
                                .param("password", "IncorrectPass")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
