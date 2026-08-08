package com.example.service.aiteacher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.api.dto.AiReportVO;
import com.example.api.dto.LearningRecordVO;
import com.example.api.dto.StudentDetailVO;
import com.example.api.entity.AbilityRecord;
import com.example.api.entity.AiReport;
import com.example.api.entity.LearningRecord;
import com.example.api.entity.Student;
import com.example.common.result.Result;
import com.example.service.dao.AbilityRecordDao;
import com.example.service.dao.AiReportDao;
import com.example.service.dao.LearningRecordDao;
import com.example.service.dao.StudentDao;
import com.example.service.aiteacher.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private static final Long DEFAULT_TEACHER_ID = 1L;
    private static final String[] ABILITY_DIMENSIONS = {"listening", "speaking", "reading", "writing", "grammar", "vocabulary"};

    private final StudentDao studentDao;
    private final LearningRecordDao learningRecordDao;
    private final AbilityRecordDao abilityRecordDao;
    private final AiReportDao aiReportDao;

    @Override
    public Page<Student> listStudents(int page, int size, String keyword, String level) {
        Page<Student> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getTeacherId, DEFAULT_TEACHER_ID);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Student::getName, keyword).or().like(Student::getEnglishName, keyword));
        }
        if (level != null && !level.isEmpty()) {
            wrapper.eq(Student::getLevel, level);
        }
        wrapper.orderByDesc(Student::getUpdatedAt);
        return studentDao.selectPage(pageParam, wrapper);
    }

    @Override
    public StudentDetailVO getStudentDetail(Long studentId) {
        Student student = studentDao.selectById(studentId);
        if (student == null) {
            return null;
        }

        StudentDetailVO vo = new StudentDetailVO();
        vo.setId(student.getId());
        vo.setName(student.getName());
        vo.setEnglishName(student.getEnglishName());
        vo.setGrade(student.getGrade());
        vo.setLevel(student.getLevel());
        vo.setGoal(student.getGoal());
        vo.setPhone(student.getPhone());
        vo.setRemark(student.getRemark());
        vo.setImpressions(student.getImpressions());
        vo.setStatus(student.getStatus());
        vo.setEnrollDate(student.getEnrollDate());

        // 计算入学天数
        if (student.getEnrollDate() != null) {
            vo.setEnrolledDays((int) ChronoUnit.DAYS.between(student.getEnrollDate(), LocalDate.now()));
        }

        // 一次性拉取该生全部能力评分，内存中计算画像与趋势，避免逐条远程查询（原逻辑 30天×6维=180 次往返）
        List<AbilityRecord> allAbilities = abilityRecordDao.selectList(
                new LambdaQueryWrapper<AbilityRecord>().eq(AbilityRecord::getStudentId, studentId));

        // 能力画像：各维度最近5次加权平均（越近权重越高）
        Map<String, List<AbilityRecord>> byDim = new LinkedHashMap<>();
        for (String dim : ABILITY_DIMENSIONS) {
            byDim.put(dim, new ArrayList<>());
        }
        for (AbilityRecord ar : allAbilities) {
            List<AbilityRecord> l = byDim.get(ar.getDimension());
            if (l != null) {
                l.add(ar);
            }
        }
        Map<String, Double> profile = new LinkedHashMap<>();
        for (String dim : ABILITY_DIMENSIONS) {
            List<AbilityRecord> recs = byDim.get(dim);
            recs.sort(Comparator.comparing(AbilityRecord::getAssessDate).reversed());
            if (!recs.isEmpty()) {
                int n = Math.min(5, recs.size());
                double weightedSum = 0;
                double weightTotal = 0;
                for (int i = 0; i < n; i++) {
                    double weight = n - i;
                    weightedSum += recs.get(i).getScore() * weight;
                    weightTotal += weight;
                }
                profile.put(dim, Math.round(weightedSum / weightTotal * 10) / 10.0);
            } else {
                profile.put(dim, 0.0);
            }
        }
        vo.setAbilityProfile(profile);

        // 最近30天能力趋势（按天×维度取平均，对齐前端趋势图）
        LocalDate trendEnd = LocalDate.now();
        LocalDate trendStart = trendEnd.minusDays(29);
        Map<String, Map<LocalDate, Double>> sumMap = new HashMap<>();
        Map<String, Map<LocalDate, Integer>> cntMap = new HashMap<>();
        for (AbilityRecord ar : allAbilities) {
            if (ar.getAssessDate() == null
                    || ar.getAssessDate().isBefore(trendStart)
                    || ar.getAssessDate().isAfter(trendEnd)) {
                continue;
            }
            sumMap.computeIfAbsent(ar.getDimension(), k -> new HashMap<>())
                  .merge(ar.getAssessDate(), (double) ar.getScore(), Double::sum);
            cntMap.computeIfAbsent(ar.getDimension(), k -> new HashMap<>())
                  .merge(ar.getAssessDate(), 1, Integer::sum);
        }
        Map<String, List<Double>> trend = new LinkedHashMap<>();
        for (String dim : ABILITY_DIMENSIONS) {
            List<Double> series = new ArrayList<>();
            LocalDate cursor = trendStart;
            Map<LocalDate, Double> sm = sumMap.get(dim);
            Map<LocalDate, Integer> cm = cntMap.get(dim);
            while (!cursor.isAfter(trendEnd)) {
                double avg = 0.0;
                Integer c = cm != null ? cm.get(cursor) : null;
                if (c != null && c > 0 && sm != null) {
                    avg = sm.getOrDefault(cursor, 0.0) / c;
                }
                series.add(Math.round(avg * 10) / 10.0);
                cursor = cursor.plusDays(1);
            }
            trend.put(dim, series);
        }
        vo.setAbilityTrend(trend);

        // 最近学习记录
        LambdaQueryWrapper<LearningRecord> lrWrapper = new LambdaQueryWrapper<>();
        lrWrapper.eq(LearningRecord::getStudentId, studentId)
                 .orderByDesc(LearningRecord::getLessonDate)
                 .last("LIMIT 10");
        List<LearningRecord> recentRecords = learningRecordDao.selectList(lrWrapper);
        vo.setRecentRecords(recentRecords.stream().map(this::toLearningRecordVO).collect(Collectors.toList()));
        vo.setTotalLessons((int) (long) learningRecordDao.selectCount(
                new LambdaQueryWrapper<LearningRecord>().eq(LearningRecord::getStudentId, studentId)));

        // 最近AI报告
        LambdaQueryWrapper<AiReport> reportWrapper = new LambdaQueryWrapper<>();
        reportWrapper.eq(AiReport::getStudentId, studentId)
                     .orderByDesc(AiReport::getCreatedAt)
                     .last("LIMIT 5");
        List<AiReport> recentReports = aiReportDao.selectList(reportWrapper);
        vo.setRecentReports(recentReports.stream().map(this::toAiReportVO).collect(Collectors.toList()));

        return vo;
    }

    @Override
    public Result<Void> addStudent(Student student) {
        student.setTeacherId(DEFAULT_TEACHER_ID);
        if (student.getStatus() == null) {
            student.setStatus(1);
        }
        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        studentDao.insert(student);
        return Result.success(null);
    }

    @Override
    public Result<Void> updateStudent(Student student) {
        student.setUpdatedAt(LocalDateTime.now());
        studentDao.updateById(student);
        return Result.success(null);
    }

    @Override
    public Result<Void> deleteStudent(Long studentId) {
        studentDao.deleteById(studentId);
        return Result.success(null);
    }

    private LearningRecordVO toLearningRecordVO(LearningRecord r) {
        LearningRecordVO vo = new LearningRecordVO();
        vo.setId(r.getId());
        vo.setStudentId(r.getStudentId());
        vo.setLessonDate(r.getLessonDate());
        vo.setCourseType(r.getCourseType());
        vo.setTopic(r.getTopic());
        vo.setKnowledgePoints(r.getKnowledgePoints());
        vo.setListeningScore(r.getListeningScore());
        vo.setSpeakingScore(r.getSpeakingScore());
        vo.setReadingScore(r.getReadingScore());
        vo.setWritingScore(r.getWritingScore());
        vo.setGrammarScore(r.getGrammarScore());
        vo.setVocabularyScore(r.getVocabularyScore());
        vo.setPerformance(r.getPerformance());
        vo.setProblemTags(r.getProblemTags());
        vo.setTeacherNote(r.getTeacherNote());
        vo.setHomeworkStatus(r.getHomeworkStatus());
        return vo;
    }

    private AiReportVO toAiReportVO(AiReport r) {
        AiReportVO vo = new AiReportVO();
        vo.setId(r.getId());
        vo.setStudentId(r.getStudentId());
        vo.setReportType(r.getReportType());
        vo.setStartDate(r.getStartDate());
        vo.setEndDate(r.getEndDate());
        vo.setTitle(r.getTitle());
        vo.setSummary(r.getSummary());
        vo.setAbilityAnalysis(r.getAbilityAnalysis());
        vo.setProblemDiagnosis(r.getProblemDiagnosis());
        vo.setTeachingSuggestion(r.getTeachingSuggestion());
        vo.setFullContent(r.getFullContent());
        vo.setStatus(r.getStatus());
        vo.setVersion(r.getVersion());
        vo.setModelName(r.getModelName());
        vo.setTokenUsage(r.getTokenUsage());
        vo.setReviewNote(r.getReviewNote());
        vo.setCreatedAt(r.getCreatedAt());
        return vo;
    }
}
