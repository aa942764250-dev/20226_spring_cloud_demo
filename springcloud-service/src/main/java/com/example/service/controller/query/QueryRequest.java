package com.example.service.controller.query;

import lombok.Data;

/**
 * 通用查询请求体。
 */
@Data
public class QueryRequest {

    /**
     * 待执行的 SQL 语句（仅允许 SELECT）。
     */
    private String sql;

    /**
     * 返回行数上限，可选，默认 100，最大 1000。
     */
    private Integer limit;
}
