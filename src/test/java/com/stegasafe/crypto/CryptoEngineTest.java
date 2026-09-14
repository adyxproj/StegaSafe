package com.stegasafe.crypto;

import com.stegasafe.exception.InvalidPasswordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoEngineTest {

    private CryptoEngine cryptoEngine;

    @BeforeEach
    void setUp() {
        cryptoEngine = new CryptoEngine();
    }

    @Test
    @DisplayName("Encrypt and decrypt standard ASCII text successfully")
    void testEncryptAndDecryptStandard() {
        String secret = "Top Secret Agent Clearance 007!";
        String password = "StrongPassword#2026";

        CryptoEngine.EncryptionOutput output = cryptoEngine.encrypt(secret, password);

        assertThat(output).isNotNull();
        assertThat(output.ciphertext()).isNotEmpty();
        assertThat(output.salt()).hasSize(CryptoEngine.SALT_LENGTH_BYTES);
        assertThat(output.iv()).hasSize(CryptoEngine.GCM_IV_LENGTH_BYTES);

        String decrypted = cryptoEngine.decrypt(output.ciphertext(), password, output.salt(), output.iv());
        assertThat(decrypted).isEqualTo(secret);
    }

    @Test
    @DisplayName("Encrypt and decrypt multi-language Unicode including Tamil and emojis")
    void testEncryptAndDecryptUnicodeAndTamil() {
        String secret = "வணக்கம் உலகம்! Secret message with Tamil & emojis: 🔐🛡️⚡ — வெற்றி நிச்சயம்!";
        String password = "ரகசிய_கடவுச்சொல்_123";

        CryptoEngine.EncryptionOutput output = cryptoEngine.encrypt(secret, password);
        String decrypted = cryptoEngine.decrypt(output.ciphertext(), password, output.salt(), output.iv());

        assertThat(decrypted).isEqualTo(secret);
    }

    @Test
    @DisplayName("Decryption with incorrect password must throw InvalidPasswordException")
    void testDecryptionWrongPassword() {
        String secret = "Highly confidential financial records";
        String password = "CorrectPassword123";
        String wrongPassword = "WrongPassword456";

        CryptoEngine.EncryptionOutput output = cryptoEngine.encrypt(secret, password);

        assertThatThrownBy(() -> cryptoEngine.decrypt(output.ciphertext(), wrongPassword, output.salt(), output.iv()))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("Incorrect password or corrupted image payload");
    }

    @Test
    @DisplayName("Decryption with tampered ciphertext must fail authentication tag check")
    void testTamperedCiphertext() {
        String secret = "Immutable message payload";
        String password = "AuthTagVerificationTest!";

        CryptoEngine.EncryptionOutput output = cryptoEngine.encrypt(secret, password);

        byte[] tamperedCiphertext = Arrays.copyOf(output.ciphertext(), output.ciphertext().length);
        // Flip one bit in ciphertext
        tamperedCiphertext[0] ^= 0x01;

        assertThatThrownBy(() -> cryptoEngine.decrypt(tamperedCiphertext, password, output.salt(), output.iv()))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    @DisplayName("Consecutive encryptions of identical plaintext generate distinct salts and IVs")
    void testUniquenessOfSaltsAndIvs() {
        String secret = "Identical message";
        String password = "SamePassword";

        CryptoEngine.EncryptionOutput out1 = cryptoEngine.encrypt(secret, password);
        CryptoEngine.EncryptionOutput out2 = cryptoEngine.encrypt(secret, password);

        assertThat(out1.salt()).isNotEqualTo(out2.salt());
        assertThat(out1.iv()).isNotEqualTo(out2.iv());
        assertThat(out1.ciphertext()).isNotEqualTo(out2.ciphertext());
    }
}
