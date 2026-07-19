package com.example.common.encrypt;

/**
 * 加密算法统一抽象接口。
 * <p>
 * 所有具体加密算法实现必须实现此接口，并通过Spring Bean注册机制自动发现和加载。
 * 加密后密文格式约定为 {@code ALGORITHM_ID:ciphertext}，如 {@code SM4:base64encodedvalue}。
 * </p>
 */
public interface EncryptAlgorithm {

    /**
     * 加密明文。
     *
     * @param plaintext 明文字符串
     * @return 密文字符串（含算法标识前缀，格式为 "ALGORITHM_ID:ciphertext"）
     */
    String encrypt(String plaintext);

    /**
     * 解密密文。
     *
     * @param ciphertext 密文字符串（含算法标识前缀）
     * @return 明文字符串
     */
    String decrypt(String ciphertext);

    /**
     * 获取算法唯一标识。
     *
     * @return 算法标识，如 "SM4"、"AES"，需全局唯一
     */
    String getAlgorithmId();
}
