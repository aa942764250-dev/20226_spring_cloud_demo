package com.example.service.aiteacher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.api.dto.LearningRecordVO;
import com.example.api.entity.AbilityRecord;
import com.example.api.entity.LearningRecord;
import com.example.api.entity.Student;
import com.example.common.result.Result;
import com.example.service.dao.AbilityRecordDao;
import com.example.service.dao.LearningRecordDao;
import com.example.service.dao.StudentDao;
import com.example.service.aiteacher.service.LearningRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl implements LearningRecordService {

    private static final Long DEFAULT_TEACHER_ID = 1L;

    private final LearningRecordDao learningRecordDao;
    private final AbilityRecordDao abilityRecordDao;
    private final StudentDao studentDao;

    @Override
    public Page<LearningRecordVO> listRecords(int page, int size, Long studentId, String startDate, String endDate) {
        Page<LearningRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<LearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningRecord::getTeacherId, DEFAULT_TEACHER_ID);
        if (studentId != null) {
            wrapper.eq(LearningRecord::getStudentId, studentId);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(LearningRecord::getLessonDate, LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(LearningRecord::getLessonDate, LocalDate.parse(endDate));
        }
        wrapper.orderByDesc(LearningRecord::getLessonDate);
        Page<LearningRecord> result = learningRecordDao.selectPage(pageParam, wrapper);

        Page<LearningRecordVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public Result<Void> addRecord(LearningRecord record) {
        record.setTeacherId(DEFAULT_TEACHER_ID);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        learningRecordDao.insert(record);

        // 自动更新能力评分：将本次6维评分写入ability_record
        saveAbilityFromRecord(record);

        return Result.success(null);
    }

    @Override
    public Result<Void> updateRecord(LearningRecord record) {
        record.setUpdatedAt(LocalDateTime.now());
        learningRecordDao.updateById(record);
        return Result.success(null);
    }

    @Override
    public Result<Void> deleteRecord(Long recordId) {
        learningRecordDao.deleteById(recordId);
        return Result.success(null);
    }

    @Override
    public Page<LearningRecordVO> getRecentRecords(Long studentId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        LambdaQueryWrapper<LearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningRecord::getStudentId, studentId)
               .between(LearningRecord::getLessonDate, startDate, endDate)
               .orderByDesc(LearningRecord::getLessonDate);
        List<LearningRecord> records = learningRecordDao.selectList(wrapper);

        Page<LearningRecordVO> page = new Page<>(1, records.size(), records.size());
        page.setRecords(records.stream().map(this::toVO).collect(Collectors.toList()));
        return page;
    }

    /**
     * 将学习记录的6维评分自动写入能力评分表
     */
    private void saveAbilityFromRecord(LearningRecord record) {
        LocalDate date = record.getLessonDate();
        Long studentId = record.getStudentId();
        saveAbility(studentId, date, "listening", record.getListeningScore(), "ai_calculated");
        saveAbility(studentId, date, "speaking", record.getSpeakingScore(), "ai_calculated");
        saveAbility(studentId, date, "reading", record.getReadingScore(), "ai_calculated");
        saveAbility(studentId, date, "writing", record.getWritingScore(), "ai_calculated");
        saveAbility(studentId, date, "grammar", record.getGrammarScore(), "ai_calculated");
        saveAbility(studentId, date, "vocabulary", record.getVocabularyScore(), "ai_calculated");
    }

    private void saveAbility(Long studentId, LocalDate date, String dimension, Integer score, String source) {
        if (score == null) {
            return;
        }
        AbilityRecord ar = new AbilityRecord();
        ar.setStudentId(studentId);
        ar.setAssessDate(date);
        ar.setDimension(dimension);
        ar.setScore(score);
        ar.setSource(source);
        ar.setCreatedAt(LocalDateTime.now());
        abilityRecordDao.insert(ar);
    }

    private LearningRecordVO toVO(LearningRecord r) {
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

        // 填充学生姓名
        Student student = studentDao.selectById(r.getStudentId());
        if (student != null) {
            vo.setStudentName(student.getName());
        }
        return vo;
    }
}
