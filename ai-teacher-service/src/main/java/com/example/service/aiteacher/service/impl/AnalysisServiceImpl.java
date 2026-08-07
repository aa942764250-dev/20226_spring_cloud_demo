package com.example.service.aiteacher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.api.entity.AbilityRecord;
import com.example.api.entity.LearningRecord;
import com.example.api.entity.Student;
import com.example.service.dao.AbilityRecordDao;
import com.example.service.dao.LearningRecordDao;
import com.example.service.dao.StudentDao;
import com.example.service.aiteacher.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 能力分析页数据实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private static final Long DEFAULT_TEACHER_ID = 1L;

    private static final LinkedHashMap<String, String> DIM_LABELS = new LinkedHashMap<>();
    static {
        DIM_LABELS.put("listening", "听力");
        DIM_LABELS.put("speaking", "口语");
        DIM_LABELS.put("reading", "阅读");
        DIM_LABELS.put("writing", "写作");
        DIM_LABELS.put("grammar", "语法");
        DIM_LABELS.put("vocabulary", "词汇");
    }

    private final StudentDao studentDao;
    private final LearningRecordDao learningRecordDao;
    private final AbilityRecordDao abilityRecordDao;

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();

        // ---- 等级分布 ----
        result.put("levelDistribution", groupCount(
                studentDao, "level as name, count(*) as `value`", "level"));

        // ---- 课程类型分布 ----
        result.put("courseDistribution", groupCount(
                learningRecordDao, "course_type as name, count(*) as `value`", "course_type"));

        // ---- 能力对比（取上课最多的前 5 名学生）----
        result.put("abilityCompare", abilityCompare());

        // ---- 近 6 个月上课频次 ----
        result.put("frequencyTrend", frequencyTrend());

        return result;
    }

    private <T> List<Map<String, Object>> groupCount(
            com.baomidou.mybatisplus.core.mapper.BaseMapper<T> dao,
            String select, String groupBy) {
        QueryWrapper<T> qw = new QueryWrapper<>();
        qw.select(select).eq("teacher_id", DEFAULT_TEACHER_ID).groupBy(groupBy);
        List<Map<String, Object>> maps = dao.selectMaps(qw);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> m : maps) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", m.get("name"));
            item.put("value", ((Number) m.get("value")).longValue());
            list.add(item);
        }
        return list;
    }

    private Map<String, Object> abilityCompare() {
        // 统计每位学生的上课次数，取前 5
        List<LearningRecord> all = learningRecordDao.selectList(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getTeacherId, DEFAULT_TEACHER_ID));
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (LearningRecord r : all) {
            counts.merge(r.getStudentId(), 1L, Long::sum);
        }
        List<Long> topIds = counts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<String> dimensions = new ArrayList<>(DIM_LABELS.values());
        List<String> students = new ArrayList<>();
        Map<String, List<Double>> values = new LinkedHashMap<>();
        for (String label : dimensions) {
            values.put(label, new ArrayList<>());
        }

        for (Long sid : topIds) {
            Student s = studentDao.selectById(sid);
            students.add(s != null ? s.getName() : ("学生" + sid));
            for (Map.Entry<String, String> e : DIM_LABELS.entrySet()) {
                String label = e.getValue();
                String key = e.getKey();
                QueryWrapper<AbilityRecord> qw = new QueryWrapper<>();
                qw.select("avg(score) as `avg`").eq("student_id", sid).eq("dimension", key);
                List<Object> objs = abilityRecordDao.selectObjs(qw);
                double avg = (objs != null && !objs.isEmpty() && objs.get(0) != null)
                        ? ((Number) objs.get(0)).doubleValue() : 0.0;
                avg = Math.round(avg * 10) / 10.0;
                values.get(label).add(avg);
            }
        }

        Map<String, Object> ac = new LinkedHashMap<>();
        ac.put("dimensions", dimensions);
        ac.put("students", students);
        ac.put("values", values);
        return ac;
    }

    private List<Map<String, Object>> frequencyTrend() {
        LocalDate now = LocalDate.now();
        LocalDate startMonth = now.minusMonths(5).withDayOfMonth(1);
        QueryWrapper<LearningRecord> qw = new QueryWrapper<>();
        qw.select("lesson_date")
          .eq("teacher_id", DEFAULT_TEACHER_ID)
          .ge("lesson_date", startMonth);
        List<Map<String, Object>> maps = learningRecordDao.selectMaps(qw);
        Map<String, Long> monthCount = new HashMap<>();
        for (Map<String, Object> m : maps) {
            Object ld = m.get("lesson_date");
            if (ld == null) {
                continue;
            }
            LocalDate d = (ld instanceof java.sql.Date)
                    ? ((java.sql.Date) ld).toLocalDate()
                    : LocalDate.parse(ld.toString());
            String key = String.format("%d-%02d", d.getYear(), d.getMonthValue());
            monthCount.merge(key, 1L, Long::sum);
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            LocalDate ym = startMonth.plusMonths(i);
            String key = String.format("%d-%02d", ym.getYear(), ym.getMonthValue());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", ym.getMonthValue() + "月");
            item.put("count", monthCount.getOrDefault(key, 0L));
            list.add(item);
        }
        return list;
    }
}
