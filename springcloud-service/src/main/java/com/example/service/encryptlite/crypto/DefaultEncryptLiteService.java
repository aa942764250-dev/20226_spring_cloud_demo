package com.example.service.encryptlite.crypto;

import com.example.service.encryptlite.config.EncryptLiteProperties;
import com.example.service.encryptlite.exception.EncryptLiteErrorCode;
import com.example.service.encryptlite.exception.EncryptLiteException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 默认AES加密服务实现。
 * <p>
 * 使用JDK内置的AES对称加密，密文通过Base64编码输出，不含算法前缀。
 * 密钥从 EncryptLiteProperties.secretKey 读取，未配置时使用默认密钥。
 * 已加密判断策略：尝试Base64解码，解码成功且解码后字节长度为16的倍数则判定为已加密。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultEncryptLiteService implements EncryptLiteService {

    private static final String ALGORITHM = "AES";
    private static final String DEFAULT_SECRET_KEY = "encryptlite12345";

    private final EncryptLiteProperties properties;

    @Override
    public String encrypt(String plaintext) {
        try {
            String key = resolveKey();
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_METHOD_ERROR, e.getMessage(), e);
        }
    }

    @Override
    public boolean isEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return decoded.length > 0 && decoded.length % 16 == 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String resolveKey() {
        String key = properties.getSecretKey();
        if (key == null || key.isEmpty()) {
            return DEFAULT_SECRET_KEY;
        }
        if (key.length() != 16) {
            log.warn("secret-key 长度非16字节，将截断或填充至16字节");
            if (key.length() > 16) {
                return key.substring(0, 16);
            }
            return key + DEFAULT_SECRET_KEY.substring(0, 16 - key.length());
        }
        return key;
    }
}