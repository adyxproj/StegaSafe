# StegaSafe 🛡️
> **Modern, High-Performance Image Steganography Web Application**  
> *Built with Java 21, Spring Boot 3, AES-256-GCM, and LSB (Least Significant Bit) Encoding.*

---

## 🌟 Overview

**StegaSafe** is an enterprise-grade, secure image steganography web application designed for students, cybersecurity researchers, and privacy-conscious users. It conceals confidential text, tokens, and multi-language messages inside digital images without causing visible color degradation.

To ensure true end-to-end security, StegaSafe combines **Least Significant Bit (LSB) steganography** with **AES-256-GCM authenticated encryption** and **PBKDF2 key derivation**.

---

## ⚡ Key Features

- **🔐 Authenticated Encryption (AES-256-GCM)**: Secret messages are encrypted using military-grade AES in Galois/Counter Mode with 128-bit authentication tags.
- **🔑 PBKDF2 Key Derivation**: Passphrases are fortified using `PBKDF2WithHmacSHA256` with 65,536 iterations and cryptographically random 16-byte salts.
- **🎨 Lossless LSB Steganography**: Sequential bit embedding into the RGB channels (3 bits per pixel) of TrueColor images.
- **🖼️ Universal Format Ingestion & Lossless Export**: Accepts PNG, JPEG, JPG, and BMP; strictly exports lossless PNG to prevent lossy DCT compression artifacts.
- **🌐 Full Unicode & Multi-Language Support**: Complete fidelity for Tamil (`வணக்கம்`), emojis (`🔐🚀`), Cyrillic, Chinese, and complex scripts.
- **📊 Real-Time Capacity Analyzer**: Instant calculation of image dimensions, pixel density, raw bit capacities, and live payload utilization warnings.
- **🛡️ Tamper & Wrong Password Detection**: Leverages AEAD tag validation and CRC32 verification to instantly detect incorrect passphrases or payload alterations.
- **⚡ In-Memory Zero Trace**: All image transformations and cryptographic operations execute directly in RAM streams—never written to disk or logs.
- **🌙 Cyberpunk / Modern Dark & Light UI**: Sleek cybersecurity interface with drag-and-drop file upload, live previews, toast notifications, and theme switching.

---

## 🏛️ System Architecture

```
                                 Browser Client
                     (HTML5 / Modern CSS / Vanilla ES6+)
                                      │
                                      ▼
                        Spring Boot 3 REST API Layer
                  ┌────────────────────────────────────────┐
                  │        SteganographyController         │
                  │   /api/steganography/{encode, decode,  │
                  │              capacity}                 │
                  └──────────────────┬─────────────────────┘
                                     │
                                     ▼
                      Core Business Services & Engines
                  ┌────────────────────────────────────────┐
                  │       LsbSteganographyService          │
                  │  - RGB Bit Embedding & BitReader       │
                  │  - Bounded Sequential Extraction       │
                  │  - ImageIO Format Normalization        │
                  │  - CRC32 Checksum Validation           │
                  └──────────────────┬─────────────────────┘
                                     │
                                     ▼
                  ┌────────────────────────────────────────┐
                  │             CryptoEngine               │
                  │  - AES-256-GCM (Cipher)                │
                  │  - PBKDF2WithHmacSHA256 (65,536 iters) │
                  │  - SecureRandom Salt (16B) & IV (12B)  │
                  └────────────────────────────────────────┘
```

---

## 📦 Binary Packet Protocol

Messages embedded inside images adhere to the following binary protocol:

| Offset (Bytes) | Field Name | Size | Description |
| :--- | :--- | :--- | :--- |
| `0 .. 3` | **Magic Header** | 4 Bytes | ASCII `STEG` (`0x53, 0x54, 0x45, 0x47`) |
| `4` | **Protocol Version** | 1 Byte | Version `0x01` |
| `5` | **Flags** | 1 Byte | Bit 0: `1` if encrypted, `0` if unencrypted |
| `6 .. 21` | **Salt** *(if encrypted)* | 16 Bytes | PBKDF2 salt generated via `SecureRandom` |
| `22 .. 33` | **IV / Nonce** *(if encrypted)* | 12 Bytes | AES-GCM 96-bit initialization vector |
| `34 .. 37` | **Payload Length** | 4 Bytes | Length of payload $N$ (Big-Endian int32) |
| `38 .. 38+N-1` | **Payload** | $N$ Bytes | Ciphertext (with 16B GCM tag) or UTF-8 text |
| `End` | **CRC32** *(if unencrypted)* | 4 Bytes | CRC32 checksum over plaintext bytes |

---

## 🚀 Getting Started

### Prerequisites
- **Java**: JDK 21 or higher (`java -version`)
- **Maven**: Maven 3.8+ (or use the included `./mvnw` wrapper)

### Run Locally
```bash
# Clone the repository
git clone https://github.com/your-username/stegasafe.git
cd stegasafe

# Run with Maven Wrapper (Windows PowerShell)
.\mvnw.cmd spring-boot:run

# Or run with standard Maven
mvn spring-boot:run
```

Once started, open your web browser and navigate to:
```
http://localhost:8080
```

---

## 🧪 Running Automated Tests

The application includes comprehensive unit and integration tests covering AES-GCM crypto, LSB embedding/decoding, Tamil/Unicode handling, capacity overflows, and MockMvc REST endpoints.

```bash
# Run all tests
mvn test
```

---

## 📡 REST API Reference

### 1. Analyze Capacity
- **Endpoint**: `POST /api/steganography/capacity`
- **Content-Type**: `multipart/form-data`
- **Parameters**: `image` (MultipartFile)
- **Response**:
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "width": 1920,
    "height": 1080,
    "totalPixels": 2073600,
    "rawCapacityBytes": 777600,
    "maxMessageBytesUnencrypted": 777586,
    "maxMessageBytesEncrypted": 777546,
    "formattedMaxCapacity": "759.3 KB",
    "filename": "wallpaper.png",
    "imageFileSizeBytes": 2489120,
    "formattedFileSize": "2.4 MB"
  }
}
```

### 2. Encode Message
- **Endpoint**: `POST /api/steganography/encode`
- **Content-Type**: `multipart/form-data`
- **Parameters**:
  - `image` (MultipartFile, required)
  - `message` (String, required)
  - `password` (String, optional)
- **Response**: Lossless PNG file stream (`image/png`)

### 3. Decode Message
- **Endpoint**: `POST /api/steganography/decode`
- **Content-Type**: `multipart/form-data`
- **Parameters**:
  - `image` (MultipartFile, required)
  - `password` (String, optional)
- **Response**:
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "success": true,
    "message": "வணக்கம் உலகம்! Secret message extracted.",
    "encrypted": true,
    "payloadSizeBytes": 68,
    "characterCount": 42,
    "extractionTimeMs": 14
  }
}
```

---

## 🔒 Security Best Practices

1. **Why PNG?** JPEG uses lossy discrete cosine transform (DCT) compression which completely wipes out least significant bits. StegaSafe always converts input images to lossless PNG.
2. **Key Derivation Work Factor**: 65,536 iterations of PBKDF2-HMAC-SHA256 resist GPU-accelerated brute force attacks.
3. **AEAD Authenticity**: GCM mode prevents tampering and bit-flipping attacks, rejecting forged or modified carrier images cleanly.

---

## 📜 License
This project is licensed under the MIT License.
