package com.example.service.controller.query;

import com.example.service.config.DynamicDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class QueryService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;
    private static final int QUERY_TIMEOUT_SECONDS = 10;

    private static final Pattern DANGEROUS_KEYWORDS = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE|GRANT|REVOKE|REPLACE|MERGE|CALL|EXEC|EXECUTE|LOAD_FILE|INTO\\s+OUTFILE|INTO\\s+DUMPFILE)\\b",
            Pattern.CASE_INSENSITIVE);

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public QueryService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcTemplate.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
    }

    /**
     * 执行查询。
     *
     * @param request 查询请求
     * @return 查询结果
     */
    @Transactional(readOnly = true, timeout = QUERY_TIMEOUT_SECONDS)
    public QueryResult execute(QueryRequest request) {
        if (request.getDatasource() != null && !request.getDatasource().trim().isEmpty()) {
            DynamicDataSource.setKey(request.getDatasource().trim());
        }

        if (request.getSql() == null || request.getSql().trim().isEmpty()) {
            throw new IllegalArgumentException("SQL 不能为空");
        }

        String rawSql = request.getSql().trim();

        // 安全校验：仅允许 SELECT
        if (!isSelectOnly(rawSql)) {
            log.warn("【通用查询】SQL 被拒绝，非 SELECT 语句: {}", rawSql);
            throw new IllegalArgumentException("仅允许 SELECT 查询语句，当前 SQL: " + rawSql);
        }

        // 行数限制
        int limitRaw = request.getLimit() == null ? DEFAULT_LIMIT : request.getLimit();
        if (limitRaw < 1) {
            limitRaw = DEFAULT_LIMIT;
        }
        if (limitRaw > MAX_LIMIT) {
            limitRaw = MAX_LIMIT;
        }
        final int limit = limitRaw;

        // 追加 LIMIT（若原 SQL 未显式指定 LIMIT）
        final String executedSql = appendLimit(rawSql, limit);

        log.info("【通用查询】执行 SQL: {}", executedSql);

        long start = System.currentTimeMillis();
        QueryResult result = new QueryResult();

        jdbcTemplate.execute((java.sql.Connection con) -> {
            try (Statement stmt = con.createStatement(
                    java.sql.ResultSet.TYPE_FORWARD_ONLY,
                    java.sql.ResultSet.CONCUR_READ_ONLY)) {
                stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                stmt.setMaxRows(limit);
                try (java.sql.ResultSet rs = stmt.executeQuery(executedSql)) {
                    java.sql.ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();

                    // 收集列名
                    List<String> columns = new ArrayList<>(columnCount);
                    for (int i = 1; i <= columnCount; i++) {
                        columns.add(meta.getColumnLabel(i));
                    }
                    result.setColumns(columns);

                    // 收集数据行
                    List<List<Object>> rows = new ArrayList<>();
                    int rowCount = 0;
                    while (rs.next() && rowCount < limit) {
                        List<Object> row = new ArrayList<>(columnCount);
                        for (int i = 1; i <= columnCount; i++) {
                            Object val = rs.getObject(i);
                            row.add(val);
                        }
                        rows.add(row);
                        rowCount++;
                    }
                    result.setRows(rows);
                    result.setRowCount(rowCount);
                }
            }
            return null;
        });

        long cost = System.currentTimeMillis() - start;
        result.setExecutedSql(executedSql);
        result.setCostMs(cost);

        log.info("【通用查询】完成: 行数={}, 耗时={}ms", result.getRowCount(), cost);
        return result;
    }

    /**
     * 判断 SQL 是否仅是 SELECT 语句。
     */
    private boolean isSelectOnly(String sql) {
        // 去注释后的纯文本
        String cleaned = sql
                .replaceAll("/\\*.*?\\*/", " ")   // 块注释
                .replaceAll("--[^\\r\\n]*", " ")   // 行注释
                .trim();
        if (cleaned.isEmpty()) {
            return false;
        }
        if (!cleaned.toUpperCase().startsWith("SELECT") && !cleaned.toUpperCase().startsWith("WITH")) {
            return false;
        }
        // 二次校验：不允许危险关键字（DDL/DML）
        if (DANGEROUS_KEYWORDS.matcher(cleaned).find()) {
            return false;
        }
        return true;
    }

    /**
     * 若 SQL 未包含 LIMIT，则追加 LIMIT n。
     * 简单处理：不解析子查询，只在末尾追加。
     */
    private String appendLimit(String sql, int limit) {
        String upper = sql.toUpperCase();
        // 已有 LIMIT 则不再追加（信任用户设置，但会被 MaxRows 二次限制）
        if (upper.contains("LIMIT")) {
            return sql;
        }
        // 去掉末尾分号再追加
        String trimmed = sql.replaceAll(";\\s*$", "");
        return trimmed + " LIMIT " + limit;
    }
}
