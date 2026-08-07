import pymysql
conn = pymysql.connect(host='154.201.68.122', port=3306, user='app_user', password='App@Remote2026#User', database='springcloud_demo', charset='utf8mb4')
cur = conn.cursor()
cur.execute("SHOW TABLES LIKE 'generation_log'")
if not cur.fetchone():
    cur.execute("""CREATE TABLE generation_log (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        target_date DATE NOT NULL,
        type VARCHAR(20) NOT NULL DEFAULT 'daily' COMMENT 'daily=每日生成 manual=手动触发 scheduled=定时触发',
        status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/running/success/failed',
        review_item_count INT NOT NULL DEFAULT 0,
        test_item_count INT NOT NULL DEFAULT 0,
        module_count INT NOT NULL DEFAULT 0,
        error_message TEXT,
        duration_ms BIGINT,
        started_at DATETIME,
        finished_at DATETIME,
        INDEX idx_target_date (target_date),
        INDEX idx_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci""")
    print('generation_log created')
else:
    print('generation_log exists')
conn.commit()
conn.close()