package com.example.service.encryptlite.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 加密初始化汇总结果DTO。
 */
@Data
public class EncryptLiteSummaryResult implements Serializable {

    /** 总处理表数 */
    private int totalTables;

    /** 成功表数（无失败记录） */
    private int successTables;

    /** 失败表数（有失败记录） */
    private int failedTables;

    /** 各表结果列表 */
    private List<EncryptLiteTableResult> tableResults;
}