package com.example.api.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 加密异常记录实体。
 * <p>
 * 对应数据库表 encrypt_task_error，记录加密过程中每条失败记录的详细信息，
 * 包括失败的字段名、记录主键、错误码和错误信息，便于问题排查和追溯。
 * </p>
 */
@Data
public class EncryptTaskError implements Serializable {

    /** 主键ID */
    private Long id;

    /** 关联的加密任务ID */
    private Long taskId;

    /** 失败时的批次号 */
    private Integer batchNo;

    /** 失败的字段名 */
    private String fieldName;

    /** 失败记录的主键值 */
    private String recordPrimaryKey;

    /** 错误码 */
    private String errorCode;

    /** 错误信息 */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createTime;
}
