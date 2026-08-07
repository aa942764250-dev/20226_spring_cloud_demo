package com.example.service.aiteacher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.api.entity.AiReport;
import com.example.api.entity.LearningRecord;
import com.example.api.entity.Student;
import com.example.service.dao.AiReportDao;
import com.example.service.dao.LearningRecordDao;
import com.example.service.dao.StudentDao;
import com.example.service.aiteacher.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

/**
 * 工作台首页数据看板实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final Long DEFAULT_TEACHER_ID = 1L;
    private static final DateTimeFormatter MD = DateTimeFormatter.ofPattern("M/d");

    private final StudentDao studentDao;
    private final LearningRecordDao learningRecordDao;
    private final AiReportDao aiReportDao;

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();

        // ---- stats ----
        long studentCount = studentDao.selectCount(
                new LambdaQueryWrapper<Student>().eq(Student::getTeacherId, DEFAULT_TEACHER_ID));
        LocalDate today = LocalDate.now();
        long todayRecords = learningRecordDao.selectCount(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getTeacherId, DEFAULT_TEACHER_ID)
                .eq(LearningRecord::getLessonDate, today));
        long pendingReports = aiReportDao.selectCount(new LambdaQueryWrapper<AiReport>()
                .eq(AiReport::getTeacherId, DEFAULT_TEACHER_ID)
                .eq(AiReport::getStatus, 1));
        long weekReports = aiReportDao.selectCount(new LambdaQueryWrapper<AiReport>()
                .eq(AiReport::getTeacherId, DEFAULT_TEACHER_ID)
                .ge(AiReport::getCreatedAt, today.minusDays(6).atStartOfDay()));

        // deltas
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        long newThisMonth = studentDao.selectCount(new LambdaQueryWrapper<Student>()
                .eq(Student::getTeacherId, DEFAULT_TEACHER_ID)
                .ge(Student::getEnrollDate, firstOfMonth));
        LocalDate yesterday = today.minusDays(1);
        long yesterdayRecords = learningRecordDao.selectCount(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getTeacherId, DEFAULT_TEACHER_ID)
                .eq(LearningRecord::getLessonDate, yesterday));
        long pendingYesterday = aiReportDao.selectCount(new LambdaQueryWrapper<AiReport>()
                .eq(AiReport::getTeacherId, DEFAULT_TEACHER_ID)
                .eq(AiReport::getStatus, 1)
                .lt(AiReport::getCreatedAt, today.atStartOfDay()));
        long prevWeekReports = aiReportDao.selectCount(new LambdaQueryWrapper<AiReport>()
                .eq(AiReport::getTeacherId, DEFAULT_TEACHER_ID)
                .between(AiReport::getCreatedAt, today.minusDays(13).atStartOfDay(), today.minusDays(7).atStartOfDay()));

        Map<String, Object> deltas = new LinkedHashMap<>();
        deltas.put("studentCount", delta(newThisMonth, "本月新增", true));
        deltas.put("todayRecords", delta(todayRecords - yesterdayRecords, "vs昨日", todayRecords >= yesterdayRecords));
        deltas.put("pendingReports", delta(pendingReports - pendingYesterday, "vs昨日", pendingReports <= pendingYesterday));
        deltas.put("weekReports", delta(weekReports - prevWeekReports, "vs上周", weekReports >= prevWeekReports));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("studentCount", studentCount);
        stats.put("todayRecords", todayRecords);
        stats.put("pendingReports", pendingReports);
        stats.put("weekReports", weekReports);
        stats.put("deltas", deltas);
        result.put("stats", stats);

        // ---- pendingRecords：最近 5 条学习记录，urgency 由六维均分推断 ----
        List<Map<String, Object>> pending = new ArrayList<>();
        List<LearningRecord> recent = learningRecordDao.selectList(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getTeacherId, DEFAULT_TEACHER_ID)
                .orderByDesc(LearningRecord::getLessonDate)
                .last("LIMIT 5"));
        for (LearningRecord r : recent) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            Student s = studentDao.selectById(r.getStudentId());
            m.put("name", s != null ? s.getName() : "未知");
            m.put("courseType", r.getCourseType());
            m.put("date", r.getLessonDate() != null ? r.getLessonDate().format(MD) : "");
            m.put("urgency", urgencyOf(r));
            pending.add(m);
        }
        result.put("pendingRecords", pending);

        // ---- recentReports：最近 5 条报告 ----
        List<Map<String, Object>> reports = new ArrayList<>();
        List<AiReport> recentReports = aiReportDao.selectList(new LambdaQueryWrapper<AiReport>()
                .eq(AiReport::getTeacherId, DEFAULT_TEACHER_ID)
                .orderByDesc(AiReport::getCreatedAt)
                .last("LIMIT 5"));
        for (AiReport r : recentReports) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("title", r.getTitle());
            m.put("status", r.getStatus());
            m.put("date", r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate().format(MD) : "");
            reports.add(m);
        }
        result.put("recentReports", reports);

        // ---- weeklyTrend：最近 7 天每日学习记录数 ----
        String[] weekNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long cnt = learningRecordDao.selectCount(new LambdaQueryWrapper<LearningRecord>()
                    .eq(LearningRecord::getTeacherId, DEFAULT_TEACHER_ID)
                    .eq(LearningRecord::getLessonDate, d));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("day", weekNames[d.getDayOfWeek().getValue() - 1]);
            m.put("count", cnt);
            trend.add(m);
        }
        result.put("weeklyTrend", trend);

        return result;
    }

    private Map<String, Object> delta(long val, String suffix, boolean up) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("val", val);
        m.put("suffix", suffix);
        m.put("up", up);
        return m;
    }

    private String urgencyOf(LearningRecord r) {
        double avg = Stream.of(r.getListeningScore(), r.getSpeakingScore(), r.getReadingScore(),
                        r.getWritingScore(), r.getGrammarScore(), r.getVocabularyScore())
                .filter(Objects::nonNull).mapToInt(Integer::intValue).average().orElse(3.0);
        if (avg < 3.0) return "high";
        if (avg < 4.0) return "mid";
        return "low";
    }
}
