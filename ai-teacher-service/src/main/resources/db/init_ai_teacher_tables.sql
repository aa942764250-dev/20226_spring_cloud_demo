-- ============================================================
-- AI 英语教师工作台 —— 数据库初始化脚本
-- 目标库：springcloud_demo（ai-teacher-service 远程 MySQL）
-- 说明：表名由 MyBatis-Plus 按类名自动映射（驼峰转下划线）。
--       使用 IF NOT EXISTS，可重复执行，已存在则跳过。
-- ============================================================

CREATE TABLE IF NOT EXISTS `student` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `teacher_id`    BIGINT       DEFAULT NULL,
  `name`          VARCHAR(64)  NOT NULL,
  `english_name`  VARCHAR(64)  DEFAULT NULL,
  `grade`         VARCHAR(32)  DEFAULT NULL,
  `level`         VARCHAR(32)  DEFAULT NULL,
  `goal`          VARCHAR(255) DEFAULT NULL,
  `phone`         VARCHAR(32)  DEFAULT NULL,
  `remark`        VARCHAR(512) DEFAULT NULL,
  `status`        INT          DEFAULT 1,
  `enroll_date`   DATE         DEFAULT NULL,
  `created_at`    DATETIME     DEFAULT NULL,
  `updated_at`    DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_student_teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `learning_record` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT,
  `teacher_id`        BIGINT       DEFAULT NULL,
  `student_id`        BIGINT       NOT NULL,
  `lesson_date`       DATE         DEFAULT NULL,
  `course_type`       VARCHAR(32)  DEFAULT NULL,
  `topic`             VARCHAR(255) DEFAULT NULL,
  `knowledge_points`  VARCHAR(512) DEFAULT NULL,
  `listening_score`   INT          DEFAULT NULL,
  `speaking_score`    INT          DEFAULT NULL,
  `reading_score`     INT          DEFAULT NULL,
  `writing_score`     INT          DEFAULT NULL,
  `grammar_score`     INT          DEFAULT NULL,
  `vocabulary_score`  INT          DEFAULT NULL,
  `performance`       VARCHAR(32)  DEFAULT NULL,
  `problem_tags`      VARCHAR(512) DEFAULT NULL,
  `teacher_note`      VARCHAR(512) DEFAULT NULL,
  `homework_status`   VARCHAR(32)  DEFAULT NULL,
  `created_at`        DATETIME     DEFAULT NULL,
  `updated_at`        DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_lr_student` (`student_id`),
  KEY `idx_lr_date` (`lesson_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `ability_record` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `student_id`    BIGINT       NOT NULL,
  `assess_date`   DATE         DEFAULT NULL,
  `dimension`     VARCHAR(32)  DEFAULT NULL,
  `score`         INT          DEFAULT NULL,
  `source`        VARCHAR(32)  DEFAULT NULL,
  `created_at`    DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ar_student_dim` (`student_id`, `dimension`),
  KEY `idx_ar_date` (`assess_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_report` (
  `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
  `teacher_id`         BIGINT       DEFAULT NULL,
  `student_id`         BIGINT       DEFAULT NULL,
  `report_type`        VARCHAR(32)  DEFAULT NULL,
  `start_date`         DATE         DEFAULT NULL,
  `end_date`           DATE         DEFAULT NULL,
  `title`              VARCHAR(255) DEFAULT NULL,
  `summary`            TEXT         DEFAULT NULL,
  `ability_analysis`   TEXT         DEFAULT NULL,
  `problem_diagnosis`  TEXT         DEFAULT NULL,
  `teaching_suggestion` TEXT        DEFAULT NULL,
  `full_content`       LONGTEXT     DEFAULT NULL,
  `status`             INT          DEFAULT 0 COMMENT '0=草稿/占位 1=待审核 2=已发布 3=已驳回 4=待生成(已入队) 5=已认领执行中',
  `version`            INT          DEFAULT 1,
  `prompt_template_id` BIGINT       DEFAULT NULL,
  `model_name`         VARCHAR(64)  DEFAULT NULL,
  `token_usage`        INT          DEFAULT NULL,
  `review_note`        VARCHAR(512) DEFAULT NULL,
  `created_at`         DATETIME     DEFAULT NULL,
  `updated_at`         DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_report_student` (`student_id`),
  KEY `idx_report_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `prompt_template` (
  `id`                   BIGINT       NOT NULL AUTO_INCREMENT,
  `name`                 VARCHAR(128) DEFAULT NULL,
  `type`                 VARCHAR(64)  DEFAULT NULL,
  `system_prompt`        TEXT         DEFAULT NULL,
  `user_prompt_template` TEXT         DEFAULT NULL,
  `output_format`        TEXT         DEFAULT NULL,
  `model_name`           VARCHAR(64)  DEFAULT NULL,
  `temperature`          DOUBLE       DEFAULT NULL,
  `max_tokens`           INT          DEFAULT NULL,
  `enabled`              INT          DEFAULT 1,
  `version`              INT          DEFAULT 1,
  `created_at`           DATETIME     DEFAULT NULL,
  `updated_at`           DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_pt_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_model_config` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `provider`   VARCHAR(32)  DEFAULT NULL,
  `model_name` VARCHAR(64)  DEFAULT NULL,
  `api_key`    VARCHAR(512) DEFAULT NULL,
  `enabled`    TINYINT      DEFAULT 0,
  `created_at` DATETIME     DEFAULT NULL,
  `updated_at` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_mc_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学生印象标签字段（2026-08 新增）：幂等加列，已存在则跳过，可重复执行
SET @exist_imp := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'student' AND COLUMN_NAME = 'impressions');
SET @sql_imp := IF(@exist_imp = 0,
  'ALTER TABLE student ADD COLUMN impressions VARCHAR(2000) DEFAULT NULL COMMENT ''深圳教研印象标签(JSON数组字符串)''',
  'SELECT 1');
PREPARE stmt_imp FROM @sql_imp;
EXECUTE stmt_imp;
DEALLOCATE PREPARE stmt_imp;
