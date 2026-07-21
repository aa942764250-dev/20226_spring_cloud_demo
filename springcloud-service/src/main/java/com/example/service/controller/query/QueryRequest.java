package com.example.service.controller.query;

import lombok.Data;

/**
 * 通用查询请求体。
 */
@Data
public class QueryRequest {

    private String sql;

    private Integer limit;

    private String datasource;
}
