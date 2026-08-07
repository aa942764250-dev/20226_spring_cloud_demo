package com.example.service.aiteacher.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.api.dto.StudentDetailVO;
import com.example.api.entity.Student;
import com.example.common.result.Result;
import com.example.service.aiteacher.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 学生管理接口
 */
@RestController
@RequestMapping("/ai-teacher/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    /** 分页查询学生列表 */
    @GetMapping("/list")
    public Result<Page<Student>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level) {
        return Result.success(studentService.listStudents(page, size, keyword, level));
    }

    /** 获取学生详情 */
    @GetMapping("/{id}")
    public Result<StudentDetailVO> detail(@PathVariable Long id) {
        StudentDetailVO vo = studentService.getStudentDetail(id);
        if (vo == null) {
            return Result.fail(404, "学生不存在");
        }
        return Result.success(vo);
    }

    /** 新增学生 */
    @PostMapping
    public Result<Void> add(@RequestBody Student student) {
        if (student.getName() == null || student.getName().isEmpty()) {
            return Result.fail("学生姓名不能为空");
        }
        return studentService.addStudent(student);
    }

    /** 更新学生 */
    @PutMapping
    public Result<Void> update(@RequestBody Student student) {
        if (student.getId() == null) {
            return Result.fail("学生ID不能为空");
        }
        return studentService.updateStudent(student);
    }

    /** 删除学生 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return studentService.deleteStudent(id);
    }
}
