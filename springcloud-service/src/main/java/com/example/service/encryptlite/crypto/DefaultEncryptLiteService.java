package com.example.service.encryptlite.crypto;

import com.example.service.encryptlite.config.EncryptLiteProperties;
import com.example.service.encryptlite.exception.EncryptLiteErrorCode;
import com.example.service.encryptlite.exception.EncryptLiteException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.security.Security;
import java.util.regex.Pattern;

/**
 * demo使用的SM4加密实现。
 * <p>
 * 使用SM4/ECB/PKCS5Padding，密文格式固定为ENC(Base64密文)。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultEncryptLiteService implements EncryptLiteService {

    private static final String ALGORITHM = "SM4";
    private static final String TRANSFORMATION = "SM4/ECB/PKCS5Padding";
    private static final String PREFIX = "ENC(";
    private static final String SUFFIX = ")";
    private static final Pattern ENC = Pattern.compile("^ENC\\([A-Za-z0-9+/=]+\\)$");

    static { Security.addProvider(new BouncyCastleProvider()); }

    private final EncryptLiteProperties properties;

    @Override
    public String encrypt(String plaintext) {
        try {
            byte[] key = resolveKey();
            SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION, "BC");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(encrypted) + SUFFIX;
        } catch (Exception e) {
            throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_METHOD_ERROR, e.getMessage(), e);
        }
    }

    @Override
    public boolean isEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return ENC.matcher(value).matches();
    }

    public void selfCheck() {
        try {
            byte[] key = resolveKey();
            SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            Cipher encCipher = Cipher.getInstance(TRANSFORMATION, "BC");
            encCipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = encCipher.doFinal("SELF_CHECK".getBytes(StandardCharsets.UTF_8));

            Cipher decCipher = Cipher.getInstance(TRANSFORMATION, "BC");
            decCipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = decCipher.doFinal(encrypted);

            if (!"SELF_CHECK".equals(new String(decrypted, StandardCharsets.UTF_8))) {
                throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_CONFIG_INVALID, "加解密自检失败：往返不一致");
            }
        } catch (EncryptLiteException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_CONFIG_INVALID, "加解密自检失败: " + e.getMessage(), e);
        }
    }

    private byte[] resolveKey() {
        String key = properties.getSecretKey();
        if (key == null || key.isEmpty()) throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_CONFIG_INVALID, "密钥未配置");
        byte[] decoded;
        try { decoded = Base64.getDecoder().decode(key); }
        catch (IllegalArgumentException e) { throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_CONFIG_INVALID, "密钥不是合法Base64"); }
        if (decoded.length != 16) throw new EncryptLiteException(EncryptLiteErrorCode.ENCRYPT_CONFIG_INVALID, "SM4密钥必须为16字节");
        return decoded;
    }
}
