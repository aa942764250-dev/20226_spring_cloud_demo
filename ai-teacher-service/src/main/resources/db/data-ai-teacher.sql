-- ============================================================
-- ai-teacher 本地联调种子数据（teacher_id 固定为 1）
-- 时间基准：2026-08-04（与联调脚本日期一致）
-- ============================================================

-- 学生（含 id=8，供 /student/8 联调）
INSERT INTO `student`(`id`,`teacher_id`,`name`,`english_name`,`grade`,`level`,`goal`,`phone`,`remark`,`status`,`enroll_date`,`created_at`,`updated_at`) VALUES
 (1,1,'张小明','Mike','初三','B1','中考口语突破','13800000001','进步明显',1,'2026-07-01',NOW(),NOW()),
 (2,1,'李华','Lisa','高二','B2','雅思6.5','13800000002','',1,'2026-07-05',NOW(),NOW()),
 (3,1,'王芳','Cindy','初一','A2','兴趣启蒙','13800000003','',1,'2026-07-10',NOW(),NOW()),
 (4,1,'陈杰','Jack','高三','C1','托福100','13800000004','',1,'2026-06-20',NOW(),NOW()),
 (5,1,'赵敏','Mia','四年级','A1','夯实基础','13800000005','',1,'2026-07-15',NOW(),NOW()),
 (6,1,'孙强','Tom','初二','A2','',13800000006,'',1,'2026-07-18',NOW(),NOW()),
 (7,1,'周婷','Tina','高一','B1','',13800000007,'',1,'2026-07-20',NOW(),NOW()),
 (8,1,'吴磊','Leo','初三','B1','冲刺名校','13800000008','重点学员',1,'2026-07-02',NOW(),NOW());

-- 学习记录：student=1 在 2026-07-29~08-04 区间有多条（保证 report/generate 与 weeklyTrend 有数据）
INSERT INTO `learning_record`(`teacher_id`,`student_id`,`lesson_date`,`course_type`,`topic`,`knowledge_points`,`listening_score`,`speaking_score`,`reading_score`,`writing_score`,`grammar_score`,`vocabulary_score`,`performance`,`problem_tags`,`teacher_note`,`homework_status`,`created_at`,`updated_at`) VALUES
 (1,1,'2026-07-29','口语','日常对话','问候与介绍',4,4,5,4,4,4,'good','发音','多练连读','done',NOW(),NOW()),
 (1,1,'2026-07-31','阅读','说明文','长难句',4,3,5,4,3,4,'normal','语法','从句分析','done',NOW(),NOW()),
 (1,1,'2026-08-02','写作','议论文','结构',3,4,4,4,4,3,'normal','词汇','高级词替换','done',NOW(),NOW()),
 (1,1,'2026-08-04','综合','阶段测试','全科',4,4,4,4,4,4,'good','',NULL,'done',NOW(),NOW()),
 (1,2,'2026-08-01','口语','演讲','逻辑',5,5,4,4,4,4,'good','',NULL,'done',NOW(),NOW()),
 (1,3,'2026-07-30','启蒙','字母与发音','音标',3,3,3,2,3,3,'normal','发音','元音','pending',NOW(),NOW()),
 (1,8,'2026-08-03','综合','名校冲刺','全科',5,5,5,5,4,5,'good','',NULL,'done',NOW(),NOW()),
-- 早期记录（供 6 个月频次/课程分布统计）
 (1,1,'2026-06-25','听力','新闻听力','笔记',3,3,4,3,3,3,'normal','',NULL,'done',NOW(),NOW()),
 (1,2,'2026-05-20','阅读','小说','推理',4,3,5,4,4,4,'good','',NULL,'done',NOW(),NOW());

-- 能力评分：student=1 六维，分布在近 30 天（供 abilityCompare / 学生画像 / 趋势）
INSERT INTO `ability_record`(`student_id`,`assess_date`,`dimension`,`score`,`source`,`created_at`) VALUES
 (1,'2026-07-10','listening',3,'测试',NOW()),
 (1,'2026-07-20','listening',4,'测试',NOW()),
 (1,'2026-08-01','listening',4,'测试',NOW()),
 (1,'2026-07-10','speaking',3,'测试',NOW()),
 (1,'2026-07-20','speaking',4,'测试',NOW()),
 (1,'2026-08-01','speaking',5,'测试',NOW()),
 (1,'2026-07-10','reading',4,'测试',NOW()),
 (1,'2026-07-20','reading',4,'测试',NOW()),
 (1,'2026-08-01','reading',5,'测试',NOW()),
 (1,'2026-07-10','writing',3,'测试',NOW()),
 (1,'2026-07-20','writing',4,'测试',NOW()),
 (1,'2026-08-01','writing',4,'测试',NOW()),
 (1,'2026-07-10','grammar',3,'测试',NOW()),
 (1,'2026-07-20','grammar',4,'测试',NOW()),
 (1,'2026-08-01','grammar',4,'测试',NOW()),
 (1,'2026-07-10','vocabulary',3,'测试',NOW()),
 (1,'2026-07-20','vocabulary',4,'测试',NOW()),
 (1,'2026-08-01','vocabulary',5,'测试',NOW()),
 (8,'2026-08-01','listening',5,'测试',NOW()),
 (8,'2026-08-01','speaking',5,'测试',NOW()),
 (8,'2026-08-01','reading',5,'测试',NOW());

-- Prompt 模板（weekly/monthly，enabled=1）
INSERT INTO `prompt_template`(`id`,`name`,`type`,`system_prompt`,`user_prompt_template`,`output_format`,`model_name`,`temperature`,`max_tokens`,`enabled`,`version`,`created_at`,`updated_at`) VALUES
 (1,'周报模板','weekly_report','你是一位专业的英语教学分析师，基于学习数据生成客观专业的周报。','请基于以下学生的学习与能力数据生成周报。','JSON',NULL,0.3,2000,1,1,NOW(),NOW()),
 (2,'月报模板','monthly_report','你是一位专业的英语教学分析师，基于学习数据生成客观专业的月报。','请基于以下学生的学习与能力数据生成月报。','JSON',NULL,0.3,2000,1,1,NOW(),NOW());

-- 示例报告（teacher_id=1, student=1, 状态=待审核 1）
INSERT INTO `ai_report`(`teacher_id`,`student_id`,`report_type`,`start_date`,`end_date`,`title`,`summary`,`ability_analysis`,`problem_diagnosis`,`teaching_suggestion`,`full_content`,`status`,`version`,`prompt_template_id`,`model_name`,`token_usage`,`review_note`,`created_at`,`updated_at`) VALUES
 (1,1,'weekly','2026-07-29','2026-08-04','张小明 周报告','【占位】AI 解析未接入。','【占位】','【占位】','【占位】','【SYSTEM_PROMPT】本地占位',1,1,1,'gemini-2.5-flash',0,'',NOW(),NOW());
