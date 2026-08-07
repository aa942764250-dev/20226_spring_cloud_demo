package com.example.service.review.generator;

import com.example.api.entity.ReviewItem;
import com.example.service.review.config.ReviewProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class SelfTestQuestionGenerator {

    private final KnowledgeSearchClient knowledgeSearchClient;
    private final ReviewProperties reviewProperties;
    private final ObjectMapper objectMapper;

    private static final LinkedHashMap<String, String[]> TEST_MODULE_QUERIES = new LinkedHashMap<>();
    static {
        TEST_MODULE_QUERIES.put("Java基础", new String[]{"Java基本类型转换", "String不可变原理", "final关键字作用"});
        TEST_MODULE_QUERIES.put("集合框架", new String[]{"HashMap底层结构", "ArrayList扩容机制", "ConcurrentHashMap分段锁"});
        TEST_MODULE_QUERIES.put("多线程与锁", new String[]{"synchronized锁升级", "ReentrantLock公平锁", "volatile可见性"});
        TEST_MODULE_QUERIES.put("线程池", new String[]{"线程池核心参数", "线程池拒绝策略对比", "线程池执行流程"});
        TEST_MODULE_QUERIES.put("JVM", new String[]{"JVM堆内存划分", "GC收集器对比", "双亲委派机制"});
        TEST_MODULE_QUERIES.put("Spring", new String[]{"Spring IOC容器初始化", "Spring AOP动态代理", "Bean作用域对比"});
        TEST_MODULE_QUERIES.put("MySQL", new String[]{"B+树索引结构", "MVCC实现原理", "SQL执行计划"});
        TEST_MODULE_QUERIES.put("Redis", new String[]{"Redis五种数据结构", "RDB与AOF对比", "Redis哨兵与集群"});
        TEST_MODULE_QUERIES.put("设计模式", new String[]{"单例模式实现方式", "工厂模式应用场景", "策略模式消除if-else"});
    }

    private static final Random RANDOM = new Random();
    private static final Pattern KEYWORD_PATTERN = Pattern.compile("([A-Za-z][A-Za-z0-9_]+(?:\\.[A-Za-z][A-Za-z0-9_]+)*)");
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("([^。；！？\n]{8,60}[。；！？])");

    public List<ReviewItem> generateTestItems(Long dailyId) {
        List<ReviewItem> items = new ArrayList<>();
        Set<String> seenQuestions = new HashSet<>();
        int sortOrder = 0;

        for (Map.Entry<String, String[]> entry : TEST_MODULE_QUERIES.entrySet()) {
            String moduleName = entry.getKey();
            for (String query : entry.getValue()) {
                List<Map<String, String>> results = knowledgeSearchClient.search(query, 2);
                for (Map<String, String> result : results) {
                    String question = result.get("question");
                    String answer = result.getOrDefault("answer", "");
                    String source = result.getOrDefault("source", "");
                    if (question == null || question.isEmpty() || seenQuestions.contains(question)) {
                        continue;
                    }
                    seenQuestions.add(question);

                    ReviewItem testItem = generateOneTestItem(dailyId, moduleName, question, answer, source, sortOrder);
                    if (testItem != null) {
                        items.add(testItem);
                        sortOrder++;
                    }
                }
            }
        }

        log.info("自测题目生成完成: dailyId={}, count={}", dailyId, items.size());
        return items;
    }

    private ReviewItem generateOneTestItem(Long dailyId, String moduleName, String question, String answer, String source, int sortOrder) {
        int typeRoll = RANDOM.nextInt(10);
        String questionType;
        String options = null;
        String correctAnswer = null;

        if (typeRoll < 4) {
            questionType = "choice";
            Map<String, Object> choiceResult = generateChoiceQuestion(question, answer);
            if (choiceResult != null) {
                options = (String) choiceResult.get("options");
                correctAnswer = (String) choiceResult.get("correctAnswer");
            } else {
                questionType = "true_false";
                Map<String, Object> tfResult = generateTrueFalseQuestion(question, answer);
                if (tfResult != null) {
                    question = (String) tfResult.get("question");
                    correctAnswer = (String) tfResult.get("correctAnswer");
                    options = (String) tfResult.get("options");
                }
            }
        } else if (typeRoll < 7) {
            questionType = "true_false";
            Map<String, Object> tfResult = generateTrueFalseQuestion(question, answer);
            if (tfResult != null) {
                question = (String) tfResult.get("question");
                correctAnswer = (String) tfResult.get("correctAnswer");
                options = (String) tfResult.get("options");
            }
        } else {
            questionType = "fill_blank";
            Map<String, Object> fbResult = generateFillBlankQuestion(question, answer);
            if (fbResult != null) {
                question = (String) fbResult.get("question");
                correctAnswer = (String) fbResult.get("correctAnswer");
            }
        }

        ReviewItem item = new ReviewItem();
        item.setDailyId(dailyId);
        item.setModuleName(moduleName);
        item.setQuestion(question);
        item.setAnswer(answer);
        item.setSortOrder(sortOrder);
        item.setSource(source);
        item.setQuestionType(questionType);
        item.setOptions(options);
        item.setCorrectAnswer(correctAnswer);
        item.setCreatedAt(java.time.LocalDateTime.now());
        return item;
    }

    private Map<String, Object> generateChoiceQuestion(String question, String answer) {
        List<String> keywords = extractKeywords(answer);
        if (keywords.size() < 2) return null;

        String correctKeyword = keywords.get(RANDOM.nextInt(keywords.size()));
        List<String> distractors = generateDistractors(correctKeyword, keywords);

        if (distractors.size() < 3) return null;

        List<String> allOptions = new ArrayList<>();
        allOptions.add(correctKeyword);
        allOptions.addAll(distractors.subList(0, Math.min(3, distractors.size())));
        Collections.shuffle(allOptions);

        String correctLabel = String.valueOf((char) ('A' + allOptions.indexOf(correctKeyword)));

        try {
            String optionsJson = objectMapper.writeValueAsString(allOptions);
            Map<String, Object> result = new HashMap<>();
            result.put("options", optionsJson);
            result.put("correctAnswer", correctLabel);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> generateTrueFalseQuestion(String question, String answer) {
        Matcher m = SENTENCE_PATTERN.matcher(answer);
        if (!m.find()) return null;

        String statement = m.group(1).trim();
        boolean isTrue = RANDOM.nextBoolean();

        String displayStatement;
        String correctAnswer;

        if (isTrue) {
            displayStatement = statement;
            correctAnswer = "T";
        } else {
            List<String> keywords = extractKeywords(answer);
            if (keywords.isEmpty()) return null;
            String keyword = keywords.get(RANDOM.nextInt(keywords.size()));
            List<String> wrongKeywords = generateDistractors(keyword, keywords);
            if (wrongKeywords.isEmpty()) return null;
            String wrongKeyword = wrongKeywords.get(0);
            displayStatement = statement.replace(keyword, wrongKeyword);
            if (displayStatement.equals(statement)) return null;
            correctAnswer = "F";
        }

        try {
            String optionsJson = objectMapper.writeValueAsString(Arrays.asList("正确(T)", "错误(F)"));
            Map<String, Object> result = new HashMap<>();
            result.put("question", "判断题：" + displayStatement);
            result.put("correctAnswer", correctAnswer);
            result.put("options", optionsJson);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> generateFillBlankQuestion(String question, String answer) {
        List<String> keywords = extractKeywords(answer);
        if (keywords.isEmpty()) return null;

        String keyword = keywords.get(RANDOM.nextInt(keywords.size()));
        if (keyword.length() < 2) return null;

        Matcher m = SENTENCE_PATTERN.matcher(answer);
        String sentence;
        if (m.find()) {
            sentence = m.group(1).trim();
        } else {
            sentence = answer.length() > 80 ? answer.substring(0, 80) : answer;
        }

        if (!sentence.contains(keyword)) return null;

        String blanked = sentence.replace(keyword, "______");
        Map<String, Object> result = new HashMap<>();
        result.put("question", "填空题：" + blanked);
        result.put("correctAnswer", keyword);
        return result;
    }

    private List<String> extractKeywords(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        Matcher m = KEYWORD_PATTERN.matcher(text);
        Set<String> seen = new HashSet<>();
        List<String> keywords = new ArrayList<>();
        while (m.find()) {
            String kw = m.group(1);
            if (kw.length() >= 3 && kw.length() <= 30 && !seen.contains(kw)) {
                seen.add(kw);
                keywords.add(kw);
            }
        }
        return keywords;
    }

    private List<String> generateDistractors(String correct, List<String> existingKeywords) {
        List<String> distractors = new ArrayList<>();
        for (String kw : existingKeywords) {
            if (!kw.equals(correct) && kw.length() >= 3) {
                distractors.add(kw);
            }
        }

        Map<String, List<String>> commonDistractors = new HashMap<>();
        commonDistractors.put("HashMap", Arrays.asList("HashTable", "TreeMap", "LinkedHashMap"));
        commonDistractors.put("ArrayList", Arrays.asList("LinkedList", "Vector", "CopyOnWriteArrayList"));
        commonDistractors.put("synchronized", Arrays.asList("volatile", "ReentrantLock", "AtomicInteger"));
        commonDistractors.put("InnoDB", Arrays.asList("MyISAM", "Memory", "Archive"));
        commonDistractors.put("serializable", Arrays.asList("externalizable", "cloneable", "comparable"));
        commonDistractors.put("JVM", Arrays.asList("JRE", "JDK", "JNI"));
        commonDistractors.put("Spring", Arrays.asList("SpringBoot", "SpringMVC", "MyBatis"));
        commonDistractors.put("Redis", Arrays.asList("Memcached", "MongoDB", "RabbitMQ"));
        commonDistractors.put("MySQL", Arrays.asList("PostgreSQL", "Oracle", "SQLite"));

        for (Map.Entry<String, List<String>> entry : commonDistractors.entrySet()) {
            if (correct.contains(entry.getKey()) || entry.getKey().contains(correct)) {
                distractors.addAll(entry.getValue());
            }
        }

        Collections.shuffle(distractors);
        return distractors.stream().distinct().limit(3).collect(java.util.stream.Collectors.toList());
    }
}