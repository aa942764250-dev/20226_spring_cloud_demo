-- ============================================
-- 工作台基础表 (workbench service)
-- ============================================
USE springcloud_demo;

-- 用户表
CREATE TABLE IF NOT EXISTS `wb_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名',
    `password` VARCHAR(128) NOT NULL COMMENT '密码(BCrypt加密)',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1正常,0禁用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作台用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `wb_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
    `role_name` VARCHAR(64) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1正常,0禁用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `wb_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 菜单表
CREATE TABLE IF NOT EXISTS `wb_menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID,0为顶级',
    `menu_name` VARCHAR(64) NOT NULL COMMENT '菜单名称',
    `menu_path` VARCHAR(128) DEFAULT NULL COMMENT '路由路径',
    `menu_icon` VARCHAR(64) DEFAULT NULL COMMENT '图标',
    `menu_type` TINYINT NOT NULL DEFAULT 1 COMMENT '类型:1目录,2菜单,3按钮',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `perms` VARCHAR(128) DEFAULT NULL COMMENT '权限标识',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1显示,0隐藏',
    `section_name` VARCHAR(64) DEFAULT NULL COMMENT '分组名称(侧边栏分组标题)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS `wb_role_menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- ============================================
-- 初始化数据
-- ============================================

-- 默认用户: admin / 123456 (BCrypt加密)
INSERT INTO `wb_user` (`username`, `password`, `nickname`, `email`, `status`) VALUES
('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '管理员', 'admin@example.com', 1),
('student', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '学员', 'student@example.com', 1);

-- 角色
INSERT INTO `wb_role` (`role_code`, `role_name`, `description`) VALUES
('ADMIN', '管理员', '拥有所有权限'),
('STUDENT', '学员', '普通学习用户');

-- 用户角色
INSERT INTO `wb_user_role` (`user_id`, `role_id`) VALUES (1, 1), (2, 2);

-- 菜单
INSERT INTO `wb_menu` (`parent_id`, `menu_name`, `menu_path`, `menu_icon`, `menu_type`, `sort_order`, `perms`, `status`, `section_name`) VALUES
-- 概览分组
(0, '概览首页', '/dashboard', 'icon-dashboard', 2, 1, 'dashboard:view', 1, '概览'),
-- 学习中心分组
(0, '知识检索', '/knowledge', 'icon-search', 2, 10, 'knowledge:view', 1, '学习中心'),
(0, '复习计划', '/review', 'icon-book', 2, 11, 'review:view', 1, '学习中心'),
(0, '今日自测', '/selftest', 'icon-edit', 2, 12, 'selftest:view', 1, '学习中心'),
(0, '错题本', '/wrong', 'icon-exclamation-circle', 2, 13, 'wrong:view', 1, '学习中心'),
-- 智能工具分组
(0, 'AI 辅导', '/ai', 'icon-robot', 2, 20, 'ai:view', 1, '智能工具');

-- 角色菜单关联 - 管理员拥有所有菜单
INSERT INTO `wb_role_menu` (`role_id`, `menu_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6);

-- 学员拥有学习相关菜单
INSERT INTO `wb_role_menu` (`role_id`, `menu_id`) VALUES
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6);
