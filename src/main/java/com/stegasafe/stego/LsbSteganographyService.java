package com.stegasafe.stego;

import com.stegasafe.crypto.CryptoEngine;
import com.stegasafe.dto.CapacityInfo;
import com.stegasafe.dto.DecodeResult;
import com.stegasafe.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Service implementing high-integrity LSB (Least Significant Bit) steganography
 * with bounded stream decoding, CRC32 integrity verification, and AES-256-GCM integration.
 */
@Service
public class LsbSteganographyService {

    private static final Logger log = LoggerFactory.getLogger(LsbSteganographyService.class);

    // Magic Bytes: "STEG" (0x53, 0x54, 0x45, 0x47)
    public static final byte[] MAGIC_BYTES = new byte[]{'S', 'T', 'E', 'G'};
    public static final byte PROTOCOL_VERSION = 0x01;
    public static final byte FLAG_ENCRYPTED = 0x01;

    // Header Overheads
    // Unencrypted: Magic(4) + Version(1) + Flags(1) + Length(4) + CRC32(4) = 14 bytes
    public static final int UNENCRYPTED_HEADER_OVERHEAD = 4 + 1 + 1 + 4 + 4;
    // Encrypted: Magic(4) + Version(1) + Flags(1) + Salt(16) + IV(12) + Length(4) + GCM_Tag(16) = 54 bytes
    public static final int ENCRYPTED_HEADER_OVERHEAD = 4 + 1 + 1 + CryptoEngine.SALT_LENGTH_BYTES + CryptoEngine.GCM_IV_LENGTH_BYTES + 4 + (CryptoEngine.GCM_TAG_BIT_LENGTH / 8);

    private final CryptoEngine cryptoEngine;

    public LsbSteganographyService(CryptoEngine cryptoEngine) {
        this.cryptoEngine = cryptoEngine;
    }

    /**
     * Calculates the message capacity of an image from a MultipartFile.
     */
    public CapacityInfo analyzeCapacity(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UnsupportedImageException("Uploaded image file is empty or missing");
        }
        BufferedImage img = readImageSafely(file);
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.png";
        return analyzeCapacity(img, filename, file.getSize());
    }

    /**
     * Calculates the message capacity from a local File.
     */
    public CapacityInfo analyzeCapacity(java.io.File file) {
        if (file == null || !file.exists()) {
            throw new UnsupportedImageException("Image file does not exist");
        }
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                throw new UnsupportedImageException("Uploaded file could not be recognized as a valid image.");
            }
            return analyzeCapacity(img, file.getName(), file.length());
        } catch (IOException e) {
            throw new UnsupportedImageException("Failed to read image: " + e.getMessage(), e);
        }
    }

    /**
     * Calculates the message capacity from a BufferedImage.
     */
    public CapacityInfo analyzeCapacity(BufferedImage img, String filename, long fileSize) {
        if (img == null) {
            throw new UnsupportedImageException("Image cannot be null");
        }
        int width = img.getWidth();
        int height = img.getHeight();
        long totalPixels = (long) width * height;
        long rawCapacityBytes = (totalPixels * 3) / 8;

        long maxUnencrypted = Math.max(0, rawCapacityBytes - UNENCRYPTED_HEADER_OVERHEAD);
        long maxEncrypted = Math.max(0, rawCapacityBytes - ENCRYPTED_HEADER_OVERHEAD);

        String name = (filename != null && !filename.isBlank()) ? filename : "image.png";
        return new CapacityInfo(
                width, height, totalPixels, rawCapacityBytes,
                maxUnencrypted, maxEncrypted,
                formatByteSize(maxEncrypted),
                name, fileSize, formatByteSize(fileSize)
        );
    }

    /**
     * Encodes a secret message into an image with optional password-based AES-256-GCM encryption.
     * Always returns lossless PNG bytes.
     */
    public byte[] encodeMessage(MultipartFile file, String message, String password) {
        if (file == null || file.isEmpty()) {
            throw new UnsupportedImageException("Image file cannot be empty");
        }
        BufferedImage sourceImage = readImageSafely(file);
        return encodeMessage(sourceImage, message, password);
    }

    /**
     * Encodes a secret message from a local File.
     */
    public byte[] encodeMessage(java.io.File file, String message, String password) {
        if (file == null || !file.exists()) {
            throw new UnsupportedImageException("Image file does not exist");
        }
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                throw new UnsupportedImageException("Invalid image: " + file.getName());
            }
            return encodeMessage(img, message, password);
        } catch (IOException e) {
            throw new UnsupportedImageException("Failed to read image: " + e.getMessage(), e);
        }
    }

    /**
     * Encodes a secret message into a BufferedImage.
     */
    public byte[] encodeMessage(BufferedImage sourceImage, String message, String password) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Secret message cannot be empty");
        }
        BufferedImage canonicalImage = toCanonicalRgb(sourceImage);

        int width = canonicalImage.getWidth();
        int height = canonicalImage.getHeight();
        long availableBytes = ((long) width * height * 3) / 8;

        boolean isEncrypted = (password != null && !password.trim().isEmpty());
        byte[] packetBytes;

        if (isEncrypted) {
            CryptoEngine.EncryptionOutput encOutput = cryptoEngine.encrypt(message, password.trim());
            byte[] ciphertext = encOutput.ciphertext();
            byte[] salt = encOutput.salt();
            byte[] iv = encOutput.iv();

            int packetSize = 4 + 1 + 1 + salt.length + iv.length + 4 + ciphertext.length;
            if (packetSize > availableBytes) {
                throw new CapacityExceededException(
                        "Message exceeds image capacity. Required: " + packetSize + " bytes, Available: " + availableBytes + " bytes",
                        packetSize, availableBytes
                );
            }

            ByteBuffer buffer = ByteBuffer.allocate(packetSize);
            buffer.put(MAGIC_BYTES);
            buffer.put(PROTOCOL_VERSION);
            buffer.put(FLAG_ENCRYPTED);
            buffer.put(salt);
            buffer.put(iv);
            buffer.putInt(ciphertext.length);
            buffer.put(ciphertext);
            packetBytes = buffer.array();
        } else {
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            CRC32 crc32 = new CRC32();
            crc32.update(messageBytes);
            long crcValue = crc32.getValue();

            int packetSize = 4 + 1 + 1 + 4 + messageBytes.length + 4;
            if (packetSize > availableBytes) {
                throw new CapacityExceededException(
                        "Message exceeds image capacity. Required: " + packetSize + " bytes, Available: " + availableBytes + " bytes",
                        packetSize, availableBytes
                );
            }

            ByteBuffer buffer = ByteBuffer.allocate(packetSize);
            buffer.put(MAGIC_BYTES);
            buffer.put(PROTOCOL_VERSION);
            buffer.put((byte) 0x00); // Not encrypted
            buffer.putInt(messageBytes.length);
            buffer.put(messageBytes);
            buffer.putInt((int) crcValue);
            packetBytes = buffer.array();
        }

        // Embed bitstream into LSB of canonicalImage
        embedBytes(canonicalImage, packetBytes);

        // Convert to PNG byte array
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            boolean written = ImageIO.write(canonicalImage, "png", baos);
            if (!written) {
                throw new SteganographyException("ImageIO failed to encode image to PNG");
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new SteganographyException("Failed to generate PNG image: " + e.getMessage(), e);
        }
    }

    /**
     * Decodes and extracts a secret message from an encoded image.
     */
    public DecodeResult decodeMessage(MultipartFile file, String password) {
        long startTime = System.currentTimeMillis();

        if (file == null || file.isEmpty()) {
            throw new UnsupportedImageException("Image file cannot be empty");
        }

        BufferedImage sourceImage = readImageSafely(file);
        BufferedImage canonicalImage = toCanonicalRgb(sourceImage);

        BitReader bitReader = new BitReader(canonicalImage);

        // Step 1: Read Magic Bytes (4 bytes)
        byte[] magic = bitReader.readBytes(4);
        if (!Arrays.equals(magic, MAGIC_BYTES)) {
            throw new NoHiddenMessageException("No StegaSafe hidden message detected in this image.");
        }

        // Step 2: Read Version (1 byte)
        byte version = bitReader.readByte();
        if (version != PROTOCOL_VERSION) {
            throw new CorruptedPayloadException("Unsupported StegaSafe protocol version: " + version);
        }

        // Step 3: Read Flags (1 byte)
        byte flags = bitReader.readByte();
        boolean isEncrypted = (flags & FLAG_ENCRYPTED) != 0;

        String secretMessage;
        int payloadSize;

        if (isEncrypted) {
            if (password == null || password.trim().isEmpty()) {
                throw new InvalidPasswordException("This image contains a password-protected message. Please enter the password.");
            }

            // Read Salt (16 bytes)
            byte[] salt = bitReader.readBytes(CryptoEngine.SALT_LENGTH_BYTES);
            // Read IV (12 bytes)
            byte[] iv = bitReader.readBytes(CryptoEngine.GCM_IV_LENGTH_BYTES);
            // Read Payload Length (4 bytes)
            payloadSize = bitReader.readInt();
            if (payloadSize <= 0 || payloadSize > bitReader.remainingBytesCapacity()) {
                throw new CorruptedPayloadException("Corrupted payload size header: " + payloadSize);
            }

            // Read Ciphertext
            byte[] ciphertext = bitReader.readBytes(payloadSize);

            // Decrypt with AES-GCM
            secretMessage = cryptoEngine.decrypt(ciphertext, password.trim(), salt, iv);
        } else {
            // Read Payload Length (4 bytes)
            payloadSize = bitReader.readInt();
            if (payloadSize <= 0 || payloadSize > bitReader.remainingBytesCapacity()) {
                throw new CorruptedPayloadException("Corrupted payload size header: " + payloadSize);
            }

            // Read Message Bytes
            byte[] messageBytes = bitReader.readBytes(payloadSize);

            // Read CRC32 (4 bytes)
            int expectedCrc = bitReader.readInt();
            CRC32 crc32 = new CRC32();
            crc32.update(messageBytes);
            if ((int) crc32.getValue() != expectedCrc) {
                throw new CorruptedPayloadException("Payload CRC32 checksum mismatch. The hidden data is corrupted.");
            }

            secretMessage = new String(messageBytes, StandardCharsets.UTF_8);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        return new DecodeResult(
                true,
                secretMessage,
                isEncrypted,
                payloadSize,
                secretMessage.length(),
                elapsed
        );
    }

    /**
     * Decodes a secret message from a local File (used by desktop app).
     */
    public DecodeResult decodeMessage(java.io.File file, String password) {
        if (file == null || !file.exists()) {
            throw new UnsupportedImageException("Image file does not exist");
        }
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                throw new UnsupportedImageException("Invalid image: " + file.getName());
            }
            return decodeMessage(img, password);
        } catch (IOException e) {
            throw new UnsupportedImageException("Failed to read image: " + e.getMessage(), e);
        }
    }

    /**
     * Decodes a secret message from a BufferedImage (used by desktop app).
     */
    public DecodeResult decodeMessage(BufferedImage sourceImage, String password) {
        long startTime = System.currentTimeMillis();
        if (sourceImage == null) {
            throw new UnsupportedImageException("Image cannot be null");
        }
        BufferedImage canonicalImage = toCanonicalRgb(sourceImage);
        BitReader bitReader = new BitReader(canonicalImage);

        byte[] magic = bitReader.readBytes(4);
        if (!Arrays.equals(magic, MAGIC_BYTES)) {
            throw new NoHiddenMessageException("No StegaSafe hidden message detected in this image.");
        }
        byte version = bitReader.readByte();
        if (version != PROTOCOL_VERSION) {
            throw new CorruptedPayloadException("Unsupported StegaSafe protocol version: " + version);
        }
        byte flags = bitReader.readByte();
        boolean isEncrypted = (flags & FLAG_ENCRYPTED) != 0;
        String secretMessage;
        int payloadSize;

        if (isEncrypted) {
            if (password == null || password.trim().isEmpty()) {
                throw new InvalidPasswordException("This image contains a password-protected message. Please enter the password.");
            }
            byte[] salt = bitReader.readBytes(CryptoEngine.SALT_LENGTH_BYTES);
            byte[] iv = bitReader.readBytes(CryptoEngine.GCM_IV_LENGTH_BYTES);
            payloadSize = bitReader.readInt();
            if (payloadSize <= 0 || payloadSize > bitReader.remainingBytesCapacity()) {
                throw new CorruptedPayloadException("Corrupted payload size header: " + payloadSize);
            }
            byte[] ciphertext = bitReader.readBytes(payloadSize);
            secretMessage = cryptoEngine.decrypt(ciphertext, password.trim(), salt, iv);
        } else {
            payloadSize = bitReader.readInt();
            if (payloadSize <= 0 || payloadSize > bitReader.remainingBytesCapacity()) {
                throw new CorruptedPayloadException("Corrupted payload size header: " + payloadSize);
            }
            byte[] messageBytes = bitReader.readBytes(payloadSize);
            int expectedCrc = bitReader.readInt();
            CRC32 crc32 = new CRC32();
            crc32.update(messageBytes);
            if ((int) crc32.getValue() != expectedCrc) {
                throw new CorruptedPayloadException("Payload CRC32 checksum mismatch. The hidden data is corrupted.");
            }
            secretMessage = new String(messageBytes, StandardCharsets.UTF_8);
        }
        long elapsed = System.currentTimeMillis() - startTime;
        return new DecodeResult(true, secretMessage, isEncrypted, payloadSize, secretMessage.length(), elapsed);
    }

    /**
     * Embeds byte array into LSB of RGB channels of canonical BufferedImage.
     */
    private void embedBytes(BufferedImage image, byte[] data) {
        int width = image.getWidth();
        int height = image.getHeight();
        int dataIndex = 0;
        int bitIndex = 0; // 0 to 7 (MSB to LSB of current byte)
        int totalBits = data.length * 8;
        int bitsWritten = 0;

        outer:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitsWritten >= totalBits) {
                    break outer;
                }

                int argb = image.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                // Embed into Red
                if (bitsWritten < totalBits) {
                    int bit = (data[dataIndex] >> (7 - bitIndex)) & 1;
                    r = (r & ~1) | bit;
                    bitsWritten++;
                    bitIndex++;
                    if (bitIndex == 8) {
                        bitIndex = 0;
                        dataIndex++;
                    }
                }

                // Embed into Green
                if (bitsWritten < totalBits) {
                    int bit = (data[dataIndex] >> (7 - bitIndex)) & 1;
                    g = (g & ~1) | bit;
                    bitsWritten++;
                    bitIndex++;
                    if (bitIndex == 8) {
                        bitIndex = 0;
                        dataIndex++;
                    }
                }

                // Embed into Blue
                if (bitsWritten < totalBits) {
                    int bit = (data[dataIndex] >> (7 - bitIndex)) & 1;
                    b = (b & ~1) | bit;
                    bitsWritten++;
                    bitIndex++;
                    if (bitIndex == 8) {
                        bitIndex = 0;
                        dataIndex++;
                    }
                }

                int newArgb = (a << 24) | (r << 16) | (g << 8) | b;
                image.setRGB(x, y, newArgb);
            }
        }
    }

    /**
     * Helper bit reader for sequential LSB pixel extraction without loading entire image into memory.
     */
    private static class BitReader {
        private final BufferedImage image;
        private final int width;
        private final int height;
        private int x = 0;
        private int y = 0;
        private int channelIndex = 0; // 0=R, 1=G, 2=B
        private int currentR, currentG, currentB;

        BitReader(BufferedImage image) {
            this.image = image;
            this.width = image.getWidth();
            this.height = image.getHeight();
            loadCurrentPixel();
        }

        private void loadCurrentPixel() {
            if (y < height) {
                int argb = image.getRGB(x, y);
                currentR = (argb >> 16) & 0xFF;
                currentG = (argb >> 8) & 0xFF;
                currentB = argb & 0xFF;
            }
        }

        private int nextBit() {
            if (y >= height) {
                throw new CorruptedPayloadException("Unexpected end of image stream while reading payload");
            }

            int bit;
            if (channelIndex == 0) {
                bit = currentR & 1;
                channelIndex = 1;
            } else if (channelIndex == 1) {
                bit = currentG & 1;
                channelIndex = 2;
            } else {
                bit = currentB & 1;
                channelIndex = 0;
                x++;
                if (x >= width) {
                    x = 0;
                    y++;
                }
                loadCurrentPixel();
            }
            return bit;
        }

        public byte readByte() {
            int b = 0;
            for (int i = 0; i < 8; i++) {
                b = (b << 1) | nextBit();
            }
            return (byte) b;
        }

        public byte[] readBytes(int length) {
            byte[] data = new byte[length];
            for (int i = 0; i < length; i++) {
                data[i] = readByte();
            }
            return data;
        }

        public int readInt() {
            int val = 0;
            for (int i = 0; i < 4; i++) {
                val = (val << 8) | (readByte() & 0xFF);
            }
            return val;
        }

        public long remainingBytesCapacity() {
            long remainingChannels = (((long) height - y) * width - x) * 3 - channelIndex;
            return remainingChannels / 8;
        }
    }

    /**
     * Converts any BufferedImage to a canonical TYPE_INT_ARGB image to normalize
     * indexed, grayscale, or atypical color spaces.
     */
    private BufferedImage toCanonicalRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB || source.getType() == BufferedImage.TYPE_INT_RGB) {
            // Even if already TYPE_INT_*, make a copy to ensure safe mutability
            BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = copy.createGraphics();
            g2d.drawImage(source, 0, 0, null);
            g2d.dispose();
            return copy;
        }

        BufferedImage canonical = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = canonical.createGraphics();
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();
        return canonical;
    }

    /**
     * Reads image from MultipartFile safely and validates dimensions.
     */
    private BufferedImage readImageSafely(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new UnsupportedImageException("Uploaded file could not be recognized as a valid image (PNG, JPEG, BMP supported).");
            }
            if (image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new UnsupportedImageException("Image has invalid dimensions: " + image.getWidth() + "x" + image.getHeight());
            }
            return image;
        } catch (IOException e) {
            throw new UnsupportedImageException("Failed to read image data: " + e.getMessage(), e);
        }
    }

    /**
     * Formats byte size into human readable string (KB, MB).
     */
    public static String formatByteSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = ("KMGTPE").charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
    }
}
