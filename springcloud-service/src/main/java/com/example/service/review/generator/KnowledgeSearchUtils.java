package com.example.service.review.generator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识库检索结果共享清洗工具。
 * 原 KnowledgeSearchClient 中的 cleanContent / extractQuestion 抽出到此，
 * 供 LocalKbServerSearcher 与 ImaKnowledgeSearcher 复用。
 *
 * 注意：NOISE 中的中文噪声正则针对早期「中文面试公众号 HTML」场景，
 * 对英文知识库无害（匹配不到，不会误删英文），保留以避免引入回归。
 * 若后续英语知识库需要更精细清洗，在此处统一调整即可。
 */
public final class KnowledgeSearchUtils {

    private KnowledgeSearchUtils() {
    }

    private static final Pattern PAGE_NO = Pattern.compile("第\\d+\\s*页\\s*共\\d+\\s*页");

    private static final Pattern[] NOISE = {
            Pattern.compile("我是被编程耽误的文艺Tom.*?。", Pattern.DOTALL),
            Pattern.compile("大家好[，,。]"),
            Pattern.compile("如果[本这]次面试解析对你有帮助[^。]*。"),
            Pattern.compile("关注我[，,][^。]*。"),
            Pattern.compile("请动动手指一键三连[^。]*。"),
            Pattern.compile("可以在评论区留言[^。]*。"),
            Pattern.compile("点赞[^。]*关注[^。]*。"),
            Pattern.compile("一个工作了\\d+\\s*年的程序员[^。]*。"),
            Pattern.compile("更多.*?请关注[^。]*。"),
            Pattern.compile("Java 全栈面试复习大全.*?目录导航"),
            Pattern.compile("[\\.·]{2,}"),
    };

    public static String cleanContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        String s = PAGE_NO.matcher(content).replaceAll("");
        for (Pattern p : NOISE) {
            s = p.matcher(s).replaceAll("");
        }
        s = s.replaceAll("[\\n\\r]+", " ").replaceAll("\\s{2,}", " ").trim();
        return s;
    }

    public static String extractQuestion(String cleaned, String query, String title) {
        if (cleaned == null || cleaned.isEmpty()) {
            return query != null && !query.isEmpty() ? query : title;
        }
        String s = cleaned.replaceFirst("^\\s*\\d+[、.．]\\s*", "").trim();
        s = s.replaceFirst("^\\s*[A-Za-z]+\\s+", "").trim();
        int qIdx = -1;
        int limit = Math.min(s.length(), 150);
        for (int i = 0; i < limit; i++) {
            char c = s.charAt(i);
            if (c == '？' || c == '?') {
                qIdx = i;
                break;
            }
        }
        if (qIdx > 5) {
            int start = 0;
            for (int i = qIdx - 1; i >= 0; i--) {
                char c = s.charAt(i);
                if (c == '。' || c == '；' || c == ';') {
                    start = i + 1;
                    break;
                }
            }
            String q = s.substring(start, qIdx + 1).trim();
            return q.length() > 80 ? q.substring(0, 80) + "…" : q;
        }
        int pIdx = -1;
        for (int i = 0; i < Math.min(s.length(), 100); i++) {
            if (s.charAt(i) == '。') {
                pIdx = i;
                break;
            }
        }
        if (pIdx > 5 && pIdx <= 40) {
            return s.substring(0, pIdx + 1).trim();
        }
        return query != null && !query.isEmpty()
                ? query
                : (title != null ? title : s.substring(0, Math.min(s.length(), 60)));
    }
}
