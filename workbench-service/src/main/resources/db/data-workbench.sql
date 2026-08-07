-- ============================================================
-- workbench 本地联调种子数据
-- 账号 admin / 123456 (BCrypt)，ADMIN 角色，工作台 + AI教师 菜单
-- ============================================================

INSERT INTO `wb_user`(`id`,`username`,`password`,`nickname`,`email`,`avatar`,`status`,`create_time`,`update_time`)
VALUES (1,'admin','$2b$12$gC5FOKMijmUFFpLTM8RXuel/YIdSF46QzT6hHBvZdZ14/wHFbx5ta','管理员','admin@demo.com','',1,NOW(),NOW());

INSERT INTO `wb_role`(`id`,`role_code`,`role_name`,`status`)
VALUES (1,'ADMIN','管理员',1);

INSERT INTO `wb_user_role`(`user_id`,`role_id`)
VALUES (1,1);

INSERT INTO `wb_menu`(`id`,`parent_id`,`menu_name`,`menu_path`,`menu_icon`,`menu_type`,`sort_order`,`section_name`,`status`)
VALUES (1,0,'工作台','/workbench','el-icon-menu',1,1,'main',1),
       (2,0,'AI教师','/ai-teacher','el-icon-magic-stick',1,2,'main',1);

INSERT INTO `wb_role_menu`(`role_id`,`menu_id`)
VALUES (1,1),(1,2);
