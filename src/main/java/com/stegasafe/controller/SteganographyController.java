package com.stegasafe.controller;

import com.stegasafe.dto.ApiResponse;
import com.stegasafe.dto.CapacityInfo;
import com.stegasafe.dto.DecodeResult;
import com.stegasafe.stego.LsbSteganographyService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/steganography")
public class SteganographyController {

    private final LsbSteganographyService steganographyService;

    public SteganographyController(LsbSteganographyService steganographyService) {
        this.steganographyService = steganographyService;
    }

    /**
     * Analyzes image dimensions and payload capacity.
     */
    @PostMapping(value = "/capacity", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CapacityInfo>> analyzeCapacity(@RequestParam("image") MultipartFile image) {
        CapacityInfo capacityInfo = steganographyService.analyzeCapacity(image);
        return ResponseEntity.ok(ApiResponse.ok(capacityInfo));
    }

    /**
     * Encodes secret message inside image. Returns lossless PNG image file.
     */
    @PostMapping(value = "/encode", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> encodeMessage(
            @RequestParam("image") MultipartFile image,
            @RequestParam("message") String message,
            @RequestParam(value = "password", required = false) String password) {

        byte[] encodedPng = steganographyService.encodeMessage(image, message, password);

        String originalName = image.getOriginalFilename();
        String outputFilename = "stegasafe_encoded.png";
        if (originalName != null && !originalName.isBlank()) {
            int lastDot = originalName.lastIndexOf('.');
            String base = (lastDot > 0) ? originalName.substring(0, lastDot) : originalName;
            outputFilename = base + "_encoded.png";
        }

        boolean isEncrypted = (password != null && !password.trim().isEmpty());

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outputFilename + "\"")
                .header("X-StegaSafe-Encrypted", String.valueOf(isEncrypted))
                .body(encodedPng);
    }

    /**
     * Extracts and decrypts secret message from an encoded image.
     */
    @PostMapping(value = "/decode", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DecodeResult>> decodeMessage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "password", required = false) String password) {

        DecodeResult result = steganographyService.decodeMessage(image, password);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
