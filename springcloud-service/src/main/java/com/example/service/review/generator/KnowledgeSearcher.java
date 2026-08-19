package com.example.service.review.generator;

import java.util.List;
import java.util.Map;

/**
 * 知识库检索后端统一接口。
 * 实现方必须把结果映射为统一结构：
 *   question / answer / source / dedupKey
 * 以保证上游（SelfTestQuestionGenerator / GenerationAsyncExecutor）消费契约不变。
 */
public interface KnowledgeSearcher {

    /**
     * 检索知识库。
     *
     * @param query 检索词
     * @param topK  返回条数上限
     * @return 统一结构列表；失败或未配置时返回空列表，不抛异常
     */
    List<Map<String, String>> search(String query, int topK);
}
