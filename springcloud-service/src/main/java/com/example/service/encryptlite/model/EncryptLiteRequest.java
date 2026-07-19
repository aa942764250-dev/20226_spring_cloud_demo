package com.example.service.encryptlite.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 加密初始化请求DTO。
 */
@Data
public class EncryptLiteRequest implements Serializable {

    /** 指定待加密的表名列表，为空时对所有配置表执行 */
    private List<String> tableNames;
}