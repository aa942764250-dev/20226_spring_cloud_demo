package com.example.service.controller.query;

import com.example.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通用 SQL 查询接口。
 * <p>
 * 在 dev 和 master profile 下启用，用于数据库调试。
 * 仅允许 SELECT 语句，强制行数限制和查询超时。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/query")
@Profile({"dev", "master"})
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    /**
     * 执行 SELECT 查询。
     *
     * @param request 查询请求
     * @return 查询结果（列名 + 数据行）
     */
    @PostMapping
    public Result<QueryResult> execute(@RequestBody QueryRequest request) {
        log.info("收到查询请求: limit={}", request.getLimit());
        return Result.success(queryService.execute(request));
    }
}
