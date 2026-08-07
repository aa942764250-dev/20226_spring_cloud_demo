-- ============================================
-- 更新名人堂数据：墨离、如花、目目、狸猫、小酒、赴北
-- ============================================

USE springcloud_demo;

-- 先确保表存在
CREATE TABLE IF NOT EXISTS `alliance_showcase` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `section`     VARCHAR(32)  NOT NULL COMMENT '板块(core_member/hall_of_fame)',
    `member_name` VARCHAR(64)  NOT NULL COMMENT '成员名称',
    `role_label`  VARCHAR(64)  DEFAULT NULL COMMENT '职务标签',
    `image_url`   VARCHAR(256) DEFAULT NULL COMMENT '头像图片路径',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '描述(名人堂用)',
    `sort_order`  INT DEFAULT 0,
    `is_active`   INT NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_section` (`section`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联盟展示位(核心成员/名人堂)';

-- 删除旧名人堂数据
DELETE FROM `alliance_showcase` WHERE `section` = 'hall_of_fame';

-- 插入新名人堂数据
INSERT INTO `alliance_showcase` (`section`, `member_name`, `role_label`, `image_url`, `description`, `sort_order`, `is_active`) VALUES
('hall_of_fame', '联盟丨墨离', NULL, '/hall-of-fame/yunhu-mubai.png',  'S14 赛季高战核心，并肩征战，共赴山河。', 1, 1),
('hall_of_fame', '联盟丨如花', NULL, '/hall-of-fame/yunhu-yanyu.png',  'S14 赛季高战核心，并肩征战，共赴山河。', 2, 1),
('hall_of_fame', '联盟丨目目', NULL, '/hall-of-fame/yunhu-menghu.png', 'S14 赛季高战核心，并肩征战，共赴山河。', 3, 1),
('hall_of_fame', '联盟丨狸猫', NULL, '/hall-of-fame/yunhu-feibo.png',  'S14 赛季高战核心，并肩征战，共赴山河。', 4, 1),
('hall_of_fame', '联盟丨小酒', NULL, '/hall-of-fame/yunhu-jianxin.png','S14 赛季高战核心，并肩征战，共赴山河。', 5, 1),
('hall_of_fame', '联盟丨赴北', NULL, '/hall-of-fame/yunhu-wanli.png',  'S14 赛季高战核心，并肩征战，共赴山河。', 6, 1);

-- 验证
SELECT * FROM `alliance_showcase` WHERE `section` = 'hall_of_fame' ORDER BY `sort_order`;