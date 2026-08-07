-- 自测清单系统 DDL
-- 数据库：springcloud_demo

-- review_item 扩展字段：支持自测题型
ALTER TABLE review_item ADD COLUMN question_type VARCHAR(20) NOT NULL DEFAULT 'review' COMMENT 'review=复习重点 fill_blank=填空 true_false=判断 choice=选择' AFTER source;
ALTER TABLE review_item ADD COLUMN options TEXT COMMENT '选择题选项JSON数组' AFTER question_type;
ALTER TABLE review_item ADD COLUMN correct_answer VARCHAR(500) COMMENT '正确答案' AFTER options;

-- 自测作答记录表
CREATE TABLE IF NOT EXISTS self_test_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    user_id VARCHAR(50) NOT NULL DEFAULT 'default',
    user_answer VARCHAR(500),
    is_correct TINYINT COMMENT '1=正确 0=错误 null=未判分',
    answered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_item_user (item_id, user_id),
    CONSTRAINT fk_answer_item FOREIGN KEY (item_id) REFERENCES review_item(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 错题本
CREATE TABLE IF NOT EXISTS wrong_answer_book (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL DEFAULT 'default',
    question VARCHAR(500) NOT NULL,
    correct_answer VARCHAR(500),
    user_answer VARCHAR(500),
    module_name VARCHAR(50),
    source VARCHAR(200),
    wrong_count INT NOT NULL DEFAULT 1,
    last_wrong_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_module (module_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 每日自测统计报告
CREATE TABLE IF NOT EXISTS self_test_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    daily_id BIGINT NOT NULL,
    user_id VARCHAR(50) NOT NULL DEFAULT 'default',
    total_count INT NOT NULL DEFAULT 0,
    correct_count INT NOT NULL DEFAULT 0,
    wrong_count INT NOT NULL DEFAULT 0,
    skip_count INT NOT NULL DEFAULT 0,
    score DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '百分制得分',
    correct_rate DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '正确率%',
    coverage_rate DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '覆盖率%(已答/总数)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_daily_user (daily_id, user_id),
    CONSTRAINT fk_report_daily FOREIGN KEY (daily_id) REFERENCES review_daily(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;