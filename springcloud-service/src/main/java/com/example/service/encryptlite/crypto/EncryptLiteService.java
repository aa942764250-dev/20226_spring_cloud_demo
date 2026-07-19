package com.example.service.encryptlite.crypto;

/**
 * 轻量加密服务接口。
 * <p>
 * 定义加密方法和已加密判断方法，默认实现为内置AES加密。
 * 替换为公共加密方法时，新增实现类并添加 @Primary 注解即可，无需修改业务代码。
 * </p>
 */
public interface EncryptLiteService {

    /**
     * 加密明文。
     *
     * @param plaintext 明文字符串
     * @return 密文字符串（纯密文，无算法前缀）
     */
    String encrypt(String plaintext);

    /**
     * 判断值是否已被加密。
     *
     * @param value 待判断的值
     * @return true=已加密，false=未加密
     */
    boolean isEncrypted(String value);
}