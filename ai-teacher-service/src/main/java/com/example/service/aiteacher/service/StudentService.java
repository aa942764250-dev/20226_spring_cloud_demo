package com.example.service.aiteacher.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.api.dto.StudentDetailVO;
import com.example.api.entity.Student;
import com.example.common.result.Result;

/**
 * 学生管理服务
 */
public interface StudentService {
    /** 分页查询学生列表 */
    Page<Student> listStudents(int page, int size, String keyword, String level);
    /** 获取学生详情（含能力画像+最近记录） */
    StudentDetailVO getStudentDetail(Long studentId);
    /** 新增学生 */
    Result<Void> addStudent(Student student);
    /** 更新学生 */
    Result<Void> updateStudent(Student student);
    /** 删除学生 */
    Result<Void> deleteStudent(Long studentId);
}
