-- 每日面试复习重点系统 DDL
-- 数据库：springcloud_demo

CREATE TABLE IF NOT EXISTS review_daily (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_date DATE NOT NULL UNIQUE,
    title VARCHAR(100) NOT NULL,
    module_count INT NOT NULL DEFAULT 0,
    item_count INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=草稿 1=已发布',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS review_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    daily_id BIGINT NOT NULL,
    module_name VARCHAR(50) NOT NULL,
    question VARCHAR(500) NOT NULL,
    answer TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    source VARCHAR(200),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_daily_id (daily_id),
    CONSTRAINT fk_item_daily FOREIGN KEY (daily_id) REFERENCES review_daily(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS review_progress (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    user_id VARCHAR(50) NOT NULL DEFAULT 'default',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=未看 1=已掌握 2=未掌握',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_item_user (item_id, user_id),
    CONSTRAINT fk_progress_item FOREIGN KEY (item_id) REFERENCES review_item(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;