-- ============================================================
-- workbench 本地联调建表（H2 MySQL 兼容模式）
-- 表结构与原远程 MySQL 一致，仅用于 local profile 自包含联调
-- ============================================================

CREATE TABLE IF NOT EXISTS `wb_user` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `username`      VARCHAR(64)  NOT NULL UNIQUE,
  `password`      VARCHAR(100) NOT NULL,
  `nickname`      VARCHAR(64),
  `email`         VARCHAR(128),
  `avatar`        VARCHAR(255),
  `status`        INT          DEFAULT 1,
  `create_time`   DATETIME,
  `update_time`   DATETIME,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `wb_role` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `role_code`  VARCHAR(64)  NOT NULL,
  `role_name`  VARCHAR(64),
  `status`     INT          DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `wb_user_role` (
  `user_id`  BIGINT,
  `role_id`  BIGINT,
  PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `wb_menu` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `parent_id`     BIGINT,
  `menu_name`     VARCHAR(64),
  `menu_path`     VARCHAR(128),
  `menu_icon`     VARCHAR(64),
  `menu_type`     INT,
  `sort_order`    INT,
  `section_name`  VARCHAR(64),
  `status`        INT          DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `wb_role_menu` (
  `role_id`  BIGINT,
  `menu_id`  BIGINT,
  PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
