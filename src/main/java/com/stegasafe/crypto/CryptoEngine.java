package com.stegasafe.crypto;

import com.stegasafe.exception.InvalidPasswordException;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * High-performance, military-grade cryptographic engine providing
 * AES-256-GCM authenticated encryption and PBKDF2 key derivation.
 */
@Component
public class CryptoEngine {

    public static final int AES_KEY_BIT_LENGTH = 256;
    public static final int GCM_IV_LENGTH_BYTES = 12;
    public static final int GCM_TAG_BIT_LENGTH = 128;
    public static final int SALT_LENGTH_BYTES = 16;
    public static final int PBKDF2_ITERATIONS = 65536;

    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final String SECRET_KEY_FACTORY_ALGO = "PBKDF2WithHmacSHA256";
    private static final String AES_ALGO = "AES";

    private final SecureRandom secureRandom = new SecureRandom();

    public record EncryptionOutput(byte[] ciphertext, byte[] salt, byte[] iv) {}

    /**
     * Encrypts plaintext string using AES-256-GCM with a PBKDF2-derived key.
     *
     * @param plaintext UTF-8 secret text to encrypt
     * @param password  User-provided passphrase
     * @return EncryptionOutput containing ciphertext (with AEAD tag), salt, and IV
     */
    public EncryptionOutput encrypt(String plaintext, String password) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Plaintext message cannot be empty");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty for encryption");
        }

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);

        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        char[] passwordChars = password.toCharArray();
        try {
            SecretKey secretKey = deriveKey(passwordChars, salt);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_BIT_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintextBytes);

            return new EncryptionOutput(ciphertext, salt, iv);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        } finally {
            Arrays.fill(passwordChars, '\0');
        }
    }

    /**
     * Decrypts AES-256-GCM ciphertext using the user-provided password, salt, and IV.
     *
     * @param ciphertext Encrypted byte payload including GCM authentication tag
     * @param password   User-provided passphrase
     * @param salt       PBKDF2 salt extracted from payload
     * @param iv         GCM initialization vector extracted from payload
     * @return Decrypted UTF-8 plaintext message
     * @throws InvalidPasswordException if key derivation or tag verification fails
     */
    public String decrypt(byte[] ciphertext, String password, byte[] salt, byte[] iv) {
        if (ciphertext == null || ciphertext.length == 0) {
            throw new IllegalArgumentException("Ciphertext cannot be empty");
        }
        if (password == null || password.isEmpty()) {
            throw new InvalidPasswordException("This image is password-protected. Please enter the password to decrypt.");
        }
        if (salt == null || salt.length != SALT_LENGTH_BYTES) {
            throw new IllegalArgumentException("Invalid cryptographic salt length");
        }
        if (iv == null || iv.length != GCM_IV_LENGTH_BYTES) {
            throw new IllegalArgumentException("Invalid cryptographic IV length");
        }

        char[] passwordChars = password.toCharArray();
        try {
            SecretKey secretKey = deriveKey(passwordChars, salt);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_BIT_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedBytes = cipher.doFinal(ciphertext);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (javax.crypto.AEADBadTagException e) {
            throw new InvalidPasswordException("Incorrect password or corrupted image payload.", e);
        } catch (Exception e) {
            throw new InvalidPasswordException("Failed to decrypt hidden message: " + e.getMessage(), e);
        } finally {
            Arrays.fill(passwordChars, '\0');
        }
    }

    /**
     * Derives a 256-bit AES key from a passphrase and salt using PBKDF2WithHmacSHA256.
     */
    private SecretKey deriveKey(char[] password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, AES_KEY_BIT_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRET_KEY_FACTORY_ALGO);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, AES_ALGO);
    }
}
