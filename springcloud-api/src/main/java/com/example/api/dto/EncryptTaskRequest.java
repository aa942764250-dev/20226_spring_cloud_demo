package com.example.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 加密任务触发请求对象。
 * <p>
 * 用于触发加密初始化任务时的请求参数，
 * tableNames 为空时表示对所有启用的配置表执行加密。
 * </p>
 */
@Data
public class EncryptTaskRequest implements Serializable {

    /** 指定待加密的表名列表，为空时对所有启用的配置表执行 */
    private List<String> tableNames;
}
