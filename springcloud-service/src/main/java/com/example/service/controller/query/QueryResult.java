package com.example.service.controller.query;

import lombok.Data;

import java.util.List;

/**
 * 通用查询返回结果。
 */
@Data
public class QueryResult {

    /**
     * 列名列表。
     */
    private List<String> columns;

    /**
     * 数据行列表，每行为 Object 列表，与 columns 顺序对应。
     */
    private List<List<Object>> rows;

    /**
     * 实际返回行数。
     */
    private Integer rowCount;

    /**
     * 实际执行的 SQL（含 LIMIT）。
     */
    private String executedSql;

    /**
     * 执行耗时（毫秒）。
     */
    private Long costMs;
}
