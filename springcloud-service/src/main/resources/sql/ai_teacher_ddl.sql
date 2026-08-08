-- ============================================================
-- AI英语教师工作台 DDL
-- 数据库：springcloud_demo
-- ============================================================

-- 1. 教师表（MVP一期预留，暂不启用鉴权）
CREATE TABLE IF NOT EXISTS teacher (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '教师姓名',
    phone VARCHAR(20) COMMENT '手机号',
    institution VARCHAR(100) COMMENT '所属机构（预留）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师表';

-- 初始化默认教师（MVP单教师模式）
INSERT IGNORE INTO teacher (id, name, status) VALUES (1, '默认教师', 1);

-- 2. 学生信息表
CREATE TABLE IF NOT EXISTS student (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL DEFAULT 1 COMMENT '教师ID（预留多账号，MVP写死1）',
    name VARCHAR(50) NOT NULL COMMENT '学生姓名',
    english_name VARCHAR(50) COMMENT '英文名',
    grade VARCHAR(20) COMMENT '年级',
    level VARCHAR(30) COMMENT '英语等级：KET/PET/剑少2等',
    goal VARCHAR(200) COMMENT '学习目标',
    phone VARCHAR(20) COMMENT '联系电话',
    remark TEXT COMMENT '备注',
    impressions VARCHAR(2000) DEFAULT NULL COMMENT '深圳教研印象标签(JSON数组字符串)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0=停课 1=在课',
    enroll_date DATE COMMENT '入学日期',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生信息表';

-- 3. 学习记录表（核心业务表）
CREATE TABLE IF NOT EXISTS learning_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL DEFAULT 1 COMMENT '教师ID（预留）',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    lesson_date DATE NOT NULL COMMENT '上课日期',
    course_type VARCHAR(20) COMMENT '课程类型：1v1/1v2/小班/大班',
    topic VARCHAR(200) COMMENT '课程主题',
    knowledge_points VARCHAR(500) COMMENT '知识点（逗号分隔）',
    listening_score TINYINT COMMENT '听力评分1-5',
    speaking_score TINYINT COMMENT '口语评分1-5',
    reading_score TINYINT COMMENT '阅读评分1-5',
    writing_score TINYINT COMMENT '写作评分1-5',
    grammar_score TINYINT COMMENT '语法评分1-5',
    vocabulary_score TINYINT COMMENT '词汇评分1-5',
    performance VARCHAR(20) COMMENT '课堂表现：excellent/good/average/needs_improvement',
    problem_tags VARCHAR(500) COMMENT '问题标签（逗号分隔）',
    teacher_note TEXT COMMENT '教师备注',
    homework_status VARCHAR(20) COMMENT '作业完成：completed/partial/not_done',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student_id (student_id),
    INDEX idx_lesson_date (lesson_date),
    INDEX idx_student_date (student_id, lesson_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习记录表';

-- 4. 能力评分记录表
CREATE TABLE IF NOT EXISTS ability_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生ID',
    assess_date DATE NOT NULL COMMENT '评估日期',
    dimension VARCHAR(20) NOT NULL COMMENT '能力维度：listening/speaking/reading/writing/grammar/vocabulary',
    score TINYINT NOT NULL COMMENT '评分1-5',
    source VARCHAR(20) NOT NULL DEFAULT 'ai_calculated' COMMENT '评分来源：manual/ai_calculated',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student_id (student_id),
    INDEX idx_dimension (dimension),
    INDEX idx_student_dim_date (student_id, dimension, assess_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='能力评分记录表';

-- 5. AI报告表
CREATE TABLE IF NOT EXISTS ai_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL DEFAULT 1 COMMENT '教师ID（预留）',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    report_type VARCHAR(20) NOT NULL COMMENT '报告类型：weekly/monthly',
    start_date DATE NOT NULL COMMENT '报告起始日期',
    end_date DATE NOT NULL COMMENT '报告结束日期',
    title VARCHAR(200) NOT NULL COMMENT '报告标题',
    summary TEXT COMMENT '学习总结（Markdown）',
    ability_analysis TEXT COMMENT '能力分析（JSON）',
    problem_diagnosis TEXT COMMENT '问题诊断（Markdown）',
    teaching_suggestion TEXT COMMENT '教学建议（Markdown）',
    full_content TEXT COMMENT '完整内容（JSON，AI原始输出）',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=草稿/占位 1=待审核 2=已发布 3=已驳回 4=待生成(已入队) 5=已认领执行中',
    version INT NOT NULL DEFAULT 1 COMMENT '版本号',
    prompt_template_id BIGINT COMMENT '使用的Prompt模板ID',
    model_name VARCHAR(50) COMMENT 'AI模型名称',
    token_usage INT COMMENT 'Token消耗',
    review_note VARCHAR(500) COMMENT '审核备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student_id (student_id),
    INDEX idx_report_type (report_type),
    INDEX idx_status (status),
    INDEX idx_student_type (student_id, report_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI报告表';

-- 6. Prompt模板表
CREATE TABLE IF NOT EXISTS prompt_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    type VARCHAR(50) NOT NULL COMMENT '模板类型：weekly_report/monthly_report/ability_analysis',
    system_prompt TEXT COMMENT 'System Prompt',
    user_prompt_template TEXT COMMENT '用户Prompt模板（含变量占位符）',
    output_format TEXT COMMENT '输出格式说明（JSON Schema描述）',
    model_name VARCHAR(50) DEFAULT 'gemini-2.5-flash' COMMENT '模型名称',
    temperature DOUBLE DEFAULT 0.7 COMMENT '温度参数',
    max_tokens INT DEFAULT 4096 COMMENT '最大输出Token',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '0=否 1=是',
    version INT NOT NULL DEFAULT 1 COMMENT '版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prompt模板表';

-- ============================================================
-- 初始Prompt模板数据
-- ============================================================

INSERT INTO prompt_template (name, type, system_prompt, user_prompt_template, model_name, temperature, max_tokens) VALUES
('周报模板V1', 'weekly_report',
'你是一位专业的英语教学分析师。基于学生的学习数据，生成客观、专业的周学习报告。

要求：
1. 基于真实学习数据，不夸大学习成果
2. 使用家长可理解的语言，避免专业术语
3. 提供可执行的教学建议，每条建议要具体到操作步骤
4. 避免无依据的评价，所有结论都要有数据支撑
5. 问题诊断要指出具体表现和可能原因
6. 输出JSON格式，包含以下字段：
   - summary: 学习总结（200-300字）
   - abilityAnalysis: 能力分析（JSON字符串，含各维度评价和趋势）
   - problemDiagnosis: 问题诊断（200-300字）
   - teachingSuggestion: 教学建议（200-300字，含具体操作步骤）',
'请为以下学生生成周报。

## 学生信息
{{student_info}}

## 报告周期
{{date_range}}

## 学习记录
{{learning_records}}

## 能力评分
{{ability_scores}}

请严格按照JSON格式输出。',
'gemini-2.5-flash', 0.7, 4096),

('月报模板V1', 'monthly_report',
'你是一位专业的英语教学分析师。基于学生一个月的学习数据，生成深度的月度分析报告。

要求：
1. 分析整月学习趋势，对比月初月末能力变化
2. 识别持续存在的问题和进步的方面
3. 给出下个月的教学重点和具体计划
4. 使用家长可理解的语言
5. 输出JSON格式，包含以下字段：
   - summary: 月度学习总结（300-500字，含趋势分析）
   - abilityAnalysis: 能力深度分析（JSON字符串，含各维度评分、趋势、对比分析）
   - problemDiagnosis: 深度问题诊断（300-500字，含根因分析）
   - teachingSuggestion: 下月教学计划（300-500字，含具体目标和步骤）',
'请为以下学生生成月报。

## 学生信息
{{student_info}}

## 报告周期
{{date_range}}

## 学习记录（共{{record_count}}次）
{{learning_records}}

## 能力评分
{{ability_scores}}

请严格按照JSON格式输出。',
'gemini-2.5-flash', 0.7, 4096);