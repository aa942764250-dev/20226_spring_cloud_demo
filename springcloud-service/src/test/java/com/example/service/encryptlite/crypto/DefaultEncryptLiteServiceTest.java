package com.example.service.encryptlite.crypto;

import com.example.service.encryptlite.config.EncryptLiteProperties;
import com.example.service.encryptlite.exception.EncryptLiteException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.Assert.*;

public class DefaultEncryptLiteServiceTest {

    private static final String VALID_KEY = Base64.getEncoder().encodeToString("1234567890abcdef".getBytes(StandardCharsets.UTF_8));

    private EncryptLiteProperties validProperties() {
        EncryptLiteProperties properties = new EncryptLiteProperties();
        properties.setSecretKey(VALID_KEY);
        return properties;
    }

    @Test
    public void shouldUseEncEnvelope() {
        DefaultEncryptLiteService service = new DefaultEncryptLiteService(validProperties());
        String encrypted = service.encrypt("13800138000");
        assertTrue(encrypted.matches("^ENC\\([A-Za-z0-9+/=]+\\)$"));
        assertTrue(service.isEncrypted(encrypted));
        assertFalse(service.isEncrypted("MTIzNDU2Nzg5MDEyMzQ1Ng=="));
    }

    @Test
    public void shouldSkipNullAndEmpty() {
        DefaultEncryptLiteService service = new DefaultEncryptLiteService(validProperties());
        assertFalse(service.isEncrypted(null));
        assertFalse(service.isEncrypted(""));
    }

    @Test
    public void shouldNotMisjudgePlainEncText() {
        DefaultEncryptLiteService service = new DefaultEncryptLiteService(validProperties());
        assertFalse(service.isEncrypted("ENC(not-valid-base64!!!)"));
        assertFalse(service.isEncrypted("ENC("));
        assertFalse(service.isEncrypted("hello"));
    }

    @Test(expected = EncryptLiteException.class)
    public void shouldRejectInvalidKey() {
        EncryptLiteProperties properties = new EncryptLiteProperties();
        properties.setSecretKey("invalid");
        new DefaultEncryptLiteService(properties).encrypt("data");
    }

    @Test
    public void shouldPassSelfCheckWithValidKey() {
        DefaultEncryptLiteService service = new DefaultEncryptLiteService(validProperties());
        service.selfCheck();
    }

    @Test(expected = EncryptLiteException.class)
    public void shouldFailSelfCheckWithWrongKeyLength() {
        EncryptLiteProperties properties = new EncryptLiteProperties();
        properties.setSecretKey(Base64.getEncoder().encodeToString("short".getBytes(StandardCharsets.UTF_8)));
        new DefaultEncryptLiteService(properties).selfCheck();
    }

    @Test(expected = EncryptLiteException.class)
    public void shouldFailSelfCheckWithEmptyKey() {
        EncryptLiteProperties properties = new EncryptLiteProperties();
        properties.setSecretKey("");
        new DefaultEncryptLiteService(properties).selfCheck();
    }

    @Test
    public void shouldEncryptAndDecryptRoundTrip() throws Exception {
        DefaultEncryptLiteService service = new DefaultEncryptLiteService(validProperties());
        String plaintext = "测试中文加密内容abc123!@#";
        String encrypted = service.encrypt(plaintext);
        assertTrue(service.isEncrypted(encrypted));

        byte[] key = Base64.getDecoder().decode(VALID_KEY);
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("SM4/ECB/PKCS5Padding", "BC");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, new javax.crypto.spec.SecretKeySpec(key, "SM4"));
        String payload = encrypted.substring("ENC(".length(), encrypted.length() - 1);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(payload));
        assertEquals(plaintext, new String(decrypted, StandardCharsets.UTF_8));
    }
}
