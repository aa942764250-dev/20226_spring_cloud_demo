package com.example.common.encrypt;

import lombok.Getter;

/**
 * 加密业务错误码枚举。
 * <p>
 * 定义加密初始化组件中所有业务异常的错误码和描述信息，
 * 用于 {@link EncryptException} 中标识具体的错误类型。
 * </p>
 */
@Getter
public enum EncryptErrorCode {

    /** 加密配置无效（表不存在、字段不存在、字段类型不匹配等） */
    ENCRYPT_CONFIG_INVALID(10001, "加密配置无效"),

    /** 加密配置格式错误（字段列表为空、批次大小超范围等） */
    ENCRYPT_CONFIG_FORMAT_ERROR(10002, "加密配置格式错误"),

    /** 同一字段在配置中重复出现 */
    ENCRYPT_FIELD_DUPLICATE(10003, "加密字段重复配置"),

    /** 指定的加密算法标识未注册 */
    ENCRYPT_ALGORITHM_NOT_FOUND(10004, "加密算法未注册"),

    /** 加密/解密方法执行失败 */
    ENCRYPT_METHOD_ERROR(10005, "加密方法执行失败"),

    /** 数据库写入密文失败 */
    ENCRYPT_DB_WRITE_ERROR(10006, "数据库写入失败"),

    /** 数据库读取明文失败 */
    ENCRYPT_DB_READ_ERROR(10007, "数据库读取失败"),

    /** 单批次处理超时 */
    ENCRYPT_BATCH_TIMEOUT(10008, "批次处理超时"),

    /** 跳过加密判断模糊（无法确定数据是否已加密） */
    ENCRYPT_SKIP_AMBIGUOUS(10009, "跳过加密判断模糊"),

    /** 同一记录部分字段加密成功、部分失败 */
    ENCRYPT_PARTIAL_FIELD_ERROR(10010, "部分字段加密失败"),

    /** 已有加密任务正在执行中，拒绝新任务 */
    ENCRYPT_TASK_RUNNING(10011, "加密任务正在执行中"),

    /** 加密进度数据损坏或异常 */
    ENCRYPT_PROGRESS_CORRUPT(10012, "加密进度数据异常"),

    /** 加密服务不可用 */
    ENCRYPT_SERVICE_UNAVAILABLE(10013, "加密服务不可用");

    /** 错误码 */
    private final int code;

    /** 错误描述 */
    private final String message;

    EncryptErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
