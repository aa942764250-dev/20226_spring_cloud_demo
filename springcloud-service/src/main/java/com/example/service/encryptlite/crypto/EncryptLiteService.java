package com.example.service.encryptlite.crypto;

/**
 * 轻量加密服务接口。
 * <p>
 * 定义存量字段加密和密文识别两个最小能力。
 * demo 默认实现使用 SM4；迁移到 eTrust 时可改为调用项目已有 ToolUtil，执行器无需感知算法细节。
 * </p>
 */
public interface EncryptLiteService {

    /**
     * 加密明文。
     *
     * @param plaintext 明文字符串
     * @return 完整版本化密文，格式为【加密：V1$Base64密文】
     */
    String encrypt(String plaintext);

    /**
     * 判断值是否已被加密。
     *
     * @param value 待判断的值
     * @return true=完整匹配V1密文，false=普通业务值；未知版本直接抛异常
     */
    boolean isEncrypted(String value);
}
