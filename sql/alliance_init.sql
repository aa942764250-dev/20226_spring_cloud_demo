-- ============================================
-- 联盟网站动态配置表 + 初始数据
-- ============================================

USE springcloud_demo;

-- 1. 通用字典表（联系方式、招募标准、赛季统计等 key-value 配置）
CREATE TABLE IF NOT EXISTS `alliance_dict` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `dict_type`   VARCHAR(64)  NOT NULL COMMENT '字典类型(contact/recruit/roster_stat等)',
    `dict_key`    VARCHAR(128) NOT NULL COMMENT '字典键',
    `dict_value`  VARCHAR(512) DEFAULT NULL COMMENT '字典值',
    `sort_order`  INT DEFAULT 0 COMMENT '排序',
    `remark`      VARCHAR(256) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_key` (`dict_type`, `dict_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联盟网站字典配置';

-- 2. 赛季名单表
CREATE TABLE IF NOT EXISTS `alliance_member` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `season`      VARCHAR(8)   NOT NULL COMMENT '赛季(S14/S15/S16)',
    `member_name` VARCHAR(64)  NOT NULL COMMENT '成员名(联盟丨红中)',
    `role_type`   VARCHAR(32)  NOT NULL DEFAULT 'MEMBER' COMMENT '角色(LEADER金主团长/MEMBER团员)',
    `sort_order`  INT DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_season` (`season`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联盟赛季名单';

-- 3. 鸣谢单位表
CREATE TABLE IF NOT EXISTS `alliance_thanks` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `unit_name`   VARCHAR(128) NOT NULL COMMENT '鸣谢单位名称',
    `sort_order`  INT DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联盟鸣谢单位';

-- ============================================
-- 初始字典数据
-- ============================================
INSERT INTO `alliance_dict` (`dict_type`, `dict_key`, `dict_value`, `sort_order`, `remark`) VALUES
-- 联系方式
('contact', 'wechat_name',  '联盟丨痞子',     1, '微信联系人名'),
('contact', 'wechat_id',    '暂未开放',       2, '微信号'),
('contact', 'qq',           '3378749891',    3, 'QQ号'),
('contact', 'email',        'postmaster@yunhutuan.cn', 4, '邮箱'),
-- 招募信息
('recruit', 'standard',     '高活即可',       1, '招新标准'),
('recruit', 'benefit',      '新人入团纵享落地费', 2, '入团权益'),
('recruit', 'benefit_detail', '具体金额添加联系方式后详谈', 3, '权益详情'),
('recruit', 'quota',        '据实际情况动态调整', 4, '招募名额'),
-- 赛季统计
('roster_stat', 'S5_total', '46', 1, 'S5总人数'),
('roster_stat', 'S5_leader','1',  2, 'S5团长数'),
('roster_stat', 'S5_sponsor','4', 3, 'S5金主数'),
('roster_stat', 'S5_member','41', 4, 'S5团员数');

-- 页面文案 (site_text)
INSERT INTO `alliance_dict` (`dict_type`, `dict_key`, `dict_value`, `sort_order`, `remark`) VALUES
('site_text', 'hero_slogan_left',  '以谋定局', 1, 'Hero主标语左'),
('site_text', 'hero_slogan_right', '以战立名', 2, 'Hero主标语右'),
('site_text', 'hero_kicker',       '盟誓山河，战无疆', 3, 'Hero副标语'),
('site_text', 'hero_intro',        '山河繁花，群雄为棋。聚高战之锋，赴世界杯之约。', 4, 'Hero介绍文案'),
('site_text', 'hero_proof_1',      'S2 创立', 5, 'Hero背书1'),
('site_text', 'hero_proof_2',      '多赛季世界杯常驻强队', 6, 'Hero背书2'),
('site_text', 'hero_proof_3',      '高活跃硬核战团', 7, 'Hero背书3'),
('site_text', 'hero_status_1',     'S2 赛季创立', 8, 'Hero状态1'),
('site_text', 'hero_status_2',     '多赛季世界杯常驻', 9, 'Hero状态2'),
('site_text', 'hero_status_3',     'S14 现役名单 36 人', 10, 'Hero状态3'),
('site_text', 'hero_status_4',     '高战力 · 高活跃', 11, 'Hero状态4'),
('site_text', 'hero_visual_caption', '郊区教父', 12, 'Hero视觉标题'),
('site_text', 'about_kicker',      'ABOUT YUNHU', 13, 'About英文标签'),
('site_text', 'about_title_main',  '盟聚山河，', 14, 'About标题主体'),
('site_text', 'about_title_em',    '繁华共铸', 15, 'About标题强调'),
('site_text', 'about_quote',       '联盟团自《三国：谋定天下》S2赛季成立以来，始终活跃于杯赛场，是高战力、高活跃玩家的聚集地。', 16, 'About引言'),
('site_text', 'about_quote_author','联盟团 · 团队宣言', 17, 'About引言署名'),
('site_text', 'tenet_1_title',     '强者聚首', 18, '团队特质1标题'),
('site_text', 'tenet_1_desc',      '以战力为锋，以执行为刃，在群雄逐鹿中并肩迎战。', 19, '团队特质1描述'),
('site_text', 'tenet_2_title',     '长久在线', 20, '团队特质2标题'),
('site_text', 'tenet_2_desc',      '稳定活跃不是口号，而是每一次集结都能彼此响应。', 21, '团队特质2描述'),
('site_text', 'tenet_3_title',     '共写史册', 22, '团队特质3标题'),
('site_text', 'tenet_3_desc',      '记录团队的赛季足迹，也记录每位成员共同走过的征程。', 23, '团队特质3描述'),
('site_text', 'chronicle_kicker',  'BATTLE CHRONICLE', 24, 'Chronicle英文标签'),
('site_text', 'chronicle_title',   '战团史册', 25, 'Chronicle标题'),
('site_text', 'chronicle_intro',   '从 S2 的第一面战旗开始，山河为卷，征途作墨。', 26, 'Chronicle介绍'),
('site_text', 'chronicle_s2_title','联盟成团', 27, 'S2章节标题'),
('site_text', 'chronicle_s2_desc', '自《三国：谋定天下》S2 赛季集结，联盟的战团史册由此开篇。', 28, 'S2章节描述'),
('site_text', 'chronicle_note',    '更多赛季征程，持续记录中。', 29, 'Chronicle待续说明'),
('site_text', 'roster_kicker',     'SEASON ROSTER', 30, 'Roster英文标签'),
('site_text', 'roster_title',      '历赛季团队名单', 31, 'Roster标题'),
('site_text', 'roster_intro',      '选择 S14、S15 或 S16，查看对应赛季的联盟团队成员档案。', 32, 'Roster介绍'),
('site_text', 'members_kicker',    'CORE MEMBERS', 33, 'Members英文标签'),
('site_text', 'members_title',     '核心成员', 34, 'Members标题'),
('site_text', 'recruit_kicker',    'RECRUITMENT ORDER', 35, 'Recruit英文标签'),
('site_text', 'recruit_title',     '同道者，共赴新局', 36, 'Recruit标题'),
('site_text', 'contact_kicker',    'CONTACT YUNHU', 37, 'Contact英文标签'),
('site_text', 'contact_title',     '与联盟并肩', 38, 'Contact标题'),
('site_text', 'contact_intro',     '通过微信、QQ或邮箱联系联盟，咨询入团条件、落地费及当前招募名额。', 39, 'Contact介绍'),
('site_text', 'hall_kicker',       'YUNHU HALL OF FAME', 40, 'Hall英文标签'),
('site_text', 'hall_title',        '名人堂', 41, 'Hall标题'),
('site_text', 'hall_intro',        '以贡献留名，以情义并肩。记录那些为联盟注入智慧、力量与温度的同袍。', 42, 'Hall介绍'),
('site_text', 'thanks_kicker',     'ACKNOWLEDGEMENTS', 43, 'Thanks英文标签'),
('site_text', 'thanks_title',      '鸣谢单位', 44, 'Thanks标题'),
('site_text', 'thanks_intro',      '感谢一路并肩、彼此照应的友团与伙伴。山河有期，来日再会。', 45, 'Thanks介绍');

-- ============================================
-- 初始名单数据（S5 共46人，S14/S15/S16 复制相同名单）
-- ============================================
INSERT INTO `alliance_member` (`season`, `member_name`, `role_type`, `sort_order`) VALUES
-- 团长
('S5', '联盟丨红中', 'LEADER', 1),
-- 金主
('S5', '联盟丨图一乐', 'SPONSOR', 2),
('S5', '联盟丨枫树', 'SPONSOR', 3),
('S5', '联盟丨老农民', 'SPONSOR', 4),
('S5', '联盟丨泡泡', 'SPONSOR', 5),
-- 团员
('S5', '联盟丨烟雨', 'MEMBER', 6),
('S5', '联盟丨拎壶冲', 'MEMBER', 7),
('S5', '联盟丨慕白', 'MEMBER', 8),
('S5', '联盟丨肥啵啵', 'MEMBER', 9),
('S5', '联盟丨剑心', 'MEMBER', 10),
('S5', '联盟丨耀耀', 'MEMBER', 11),
('S5', '联盟丨万里', 'MEMBER', 12),
('S5', '联盟丨祈欧', 'MEMBER', 13),
('S5', '联盟丨粥粥', 'MEMBER', 14),
('S5', '联盟丨海', 'MEMBER', 15),
('S5', '联盟丨轩辕', 'MEMBER', 16),
('S5', '联盟丨水怪', 'MEMBER', 17),
('S5', '联盟丨玉京子', 'MEMBER', 18),
('S5', '联盟丨祖龙', 'MEMBER', 19),
('S5', '联盟丨秣陵', 'MEMBER', 20),
('S5', '联盟丨大帅鲲', 'MEMBER', 21),
('S5', '联盟丨魍魉', 'MEMBER', 22),
('S5', '联盟丨杰克', 'MEMBER', 23),
('S5', '联盟丨青山', 'MEMBER', 24),
('S5', '联盟丨青龙', 'MEMBER', 25),
('S5', '联盟丨苍灵', 'MEMBER', 26),
('S5', '联盟丨豌豆', 'MEMBER', 27),
('S5', '联盟丨天涯', 'MEMBER', 28),
('S5', '联盟丨无忧', 'MEMBER', 29),
('S5', '联盟丨妙才', 'MEMBER', 30),
('S5', '联盟丨碎碎念', 'MEMBER', 31),
('S5', '联盟丨文远', 'MEMBER', 32),
('S5', '联盟丨瓶盖', 'MEMBER', 33),
('S5', '联盟丨小泡泡', 'MEMBER', 34),
('S5', '联盟丨娜扎', 'MEMBER', 35),
('S5', '联盟丨王麻子', 'MEMBER', 36),
('S5', '联盟丨魏武', 'MEMBER', 37),
('S5', '联盟丨七煞', 'MEMBER', 38),
('S5', '联盟丨季夏', 'MEMBER', 39),
('S5', '联盟丨秋月', 'MEMBER', 40),
('S5', '联盟丨山巅自相逢', 'MEMBER', 41),
('S5', '联盟丨大茶', 'MEMBER', 42),
('S5', '联盟丨九千', 'MEMBER', 43),
('S5', '联盟丨不坚强', 'MEMBER', 44),
('S5', '联盟丨萌萌', 'MEMBER', 45),
('S5', '联盟丨旧城以西', 'MEMBER', 46);

-- 4. 展示位表（核心成员 / 名人堂）
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

-- ============================================
-- 核心成员数据
-- ============================================
INSERT INTO `alliance_showcase` (`section`, `member_name`, `role_label`, `image_url`, `sort_order`, `is_active`) VALUES
('core_member', '联盟丨痞子', '团长',   '/core-members/yunhu-hongzhong.jpg', 1, 1),
('core_member', '联盟丨赛文', '副团长', '/core-members/yunhu-paopao.jpg',    2, 1),
('core_member', '联盟丨枫树', '元老',   '/core-members/yunhu-fengshu.jpg',   3, 1),
('core_member', '联盟丨图一乐', '元老', '/core-members/yunhu-tuyile.jpg',    4, 1),
('core_member', '联盟丨老农民', '元老', '/core-members/yunhu-laonongmin.jpg', 5, 1),
('core_member', '联盟丨泡泡', '核心',   '/core-members/yunhu-paopao.jpg',    6, 1);

-- ============================================
-- 名人堂数据
-- ============================================
INSERT INTO `alliance_showcase` (`section`, `member_name`, `role_label`, `image_url`, `description`, `sort_order`, `is_active`) VALUES
('hall_of_fame', '联盟丨墨离', NULL, '/hall-of-fame/yunhu-mubai.png',  '资深 SLG 玩家，始终以敏锐判断与丰富经验，为团队发展提出高质量建议。', 1, 1),
('hall_of_fame', '联盟丨如花', NULL, '/hall-of-fame/yunhu-yanyu.png',  '团队配将负责人，无论王业还是演武，都能结合战局给出可靠、优秀的配将方案。', 2, 1),
('hall_of_fame', '联盟丨目目', NULL, '/hall-of-fame/yunhu-menghu.png', '只发红包不充钱', 3, 1),
('hall_of_fame', '联盟丨狸猫', NULL, '/hall-of-fame/yunhu-feibo.png',  '联盟招生办负责人，统筹成员招募与新人接引，为团队持续汇聚新力量。', 4, 1),
('hall_of_fame', '联盟丨小酒', NULL, '/hall-of-fame/yunhu-jianxin.png','对多个行业见解颇深，善于把复杂问题讲透，帮助大家少走弯路。', 5, 1),
('hall_of_fame', '联盟丨赴北', NULL, '/hall-of-fame/yunhu-wanli.png',  '团队氛围的缔造者，总能让并肩作战之外的联盟保持热闹与温度。', 6, 1);

-- ============================================
-- 鸣谢单位
-- ============================================
INSERT INTO `alliance_thanks` (`unit_name`, `sort_order`) VALUES
('霄顶天宫', 1),
('北境', 2),
('霖梦泽', 3),
('淮水竹亭', 4),
('疯狂动物城', 5),
('天丨李锦记', 6),
('光武丨大核', 7);
