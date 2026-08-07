import pymysql

conn = pymysql.connect(host='154.201.68.122', port=3306, user='app_user', password='App@Remote2026#User', database='springcloud_demo', charset='utf8mb4')
cur = conn.cursor()

cur.execute("SHOW COLUMNS FROM review_item LIKE 'question_type'")
if not cur.fetchone():
    cur.execute("ALTER TABLE review_item ADD COLUMN question_type VARCHAR(20) NOT NULL DEFAULT 'review' COMMENT 'review/fill_blank/true_false/choice' AFTER source")
    cur.execute("ALTER TABLE review_item ADD COLUMN options TEXT COMMENT 'choice options JSON' AFTER question_type")
    cur.execute("ALTER TABLE review_item ADD COLUMN correct_answer VARCHAR(500) COMMENT 'correct answer' AFTER options")
    print('review_item extended')
else:
    print('review_item columns already exist')

cur.execute("SHOW TABLES LIKE 'self_test_answer'")
if not cur.fetchone():
    cur.execute("""CREATE TABLE self_test_answer (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        item_id BIGINT NOT NULL,
        user_id VARCHAR(50) NOT NULL DEFAULT 'default',
        user_answer VARCHAR(500),
        is_correct TINYINT,
        answered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        UNIQUE INDEX uk_item_user (item_id, user_id),
        CONSTRAINT fk_answer_item FOREIGN KEY (item_id) REFERENCES review_item(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci""")
    print('self_test_answer created')
else:
    print('self_test_answer exists')

cur.execute("SHOW TABLES LIKE 'wrong_answer_book'")
if not cur.fetchone():
    cur.execute("""CREATE TABLE wrong_answer_book (
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
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci""")
    print('wrong_answer_book created')
else:
    print('wrong_answer_book exists')

cur.execute("SHOW TABLES LIKE 'self_test_report'")
if not cur.fetchone():
    cur.execute("""CREATE TABLE self_test_report (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        daily_id BIGINT NOT NULL,
        user_id VARCHAR(50) NOT NULL DEFAULT 'default',
        total_count INT NOT NULL DEFAULT 0,
        correct_count INT NOT NULL DEFAULT 0,
        wrong_count INT NOT NULL DEFAULT 0,
        skip_count INT NOT NULL DEFAULT 0,
        score DECIMAL(5,2) NOT NULL DEFAULT 0,
        correct_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
        coverage_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        UNIQUE INDEX uk_daily_user (daily_id, user_id),
        CONSTRAINT fk_report_daily FOREIGN KEY (daily_id) REFERENCES review_daily(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci""")
    print('self_test_report created')
else:
    print('self_test_report exists')

conn.commit()
conn.close()
print('DDL done')