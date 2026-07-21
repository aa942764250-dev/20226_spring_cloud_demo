CREATE DATABASE IF NOT EXISTS springcloud_demo DEFAULT CHARACTER SET utf8mb4;

USE springcloud_demo;

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 客户表：含姓名/身份证/手机/邮箱/地址/银行卡等典型加密字段
CREATE TABLE IF NOT EXISTS `customer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `customer_no` VARCHAR(32) NOT NULL COMMENT '客户编号',
    `customer_name` VARCHAR(128) DEFAULT NULL COMMENT '客户姓名',
    `id_card` VARCHAR(256) DEFAULT NULL COMMENT '身份证号',
    `phone` VARCHAR(256) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(256) DEFAULT NULL COMMENT '邮箱',
    `bank_card_no` VARCHAR(256) DEFAULT NULL COMMENT '银行卡号',
    `address` VARCHAR(512) DEFAULT NULL COMMENT '家庭住址',
    `status` VARCHAR(16) DEFAULT 'ACTIVE' COMMENT '状态',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_customer_no` (`customer_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- 员工表：含姓名/身份证/手机/薪资/银行卡等
CREATE TABLE IF NOT EXISTS `employee` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `emp_no` VARCHAR(32) NOT NULL COMMENT '工号',
    `emp_name` VARCHAR(128) DEFAULT NULL COMMENT '员工姓名',
    `id_card` VARCHAR(256) DEFAULT NULL COMMENT '身份证号',
    `phone` VARCHAR(256) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(256) DEFAULT NULL COMMENT '邮箱',
    `salary` VARCHAR(256) DEFAULT NULL COMMENT '月薪(加密存储)',
    `bank_card_no` VARCHAR(256) DEFAULT NULL COMMENT '工资卡号',
    `department` VARCHAR(64) DEFAULT NULL COMMENT '部门',
    `position` VARCHAR(64) DEFAULT NULL COMMENT '职位',
    `hire_date` DATE DEFAULT NULL COMMENT '入职日期',
    `status` VARCHAR(16) DEFAULT 'ACTIVE' COMMENT '状态',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emp_no` (`emp_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

-- 订单表：含收货人/手机/地址/支付账号等
CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `customer_id` BIGINT DEFAULT NULL COMMENT '客户ID',
    `receiver_name` VARCHAR(128) DEFAULT NULL COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(256) DEFAULT NULL COMMENT '收货人手机',
    `receiver_address` VARCHAR(512) DEFAULT NULL COMMENT '收货地址',
    `pay_account` VARCHAR(256) DEFAULT NULL COMMENT '支付账号',
    `total_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '订单金额',
    `order_status` VARCHAR(32) DEFAULT 'CREATED' COMMENT '订单状态',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 合同表：含甲方联系人/手机/身份证/合同金额等
CREATE TABLE IF NOT EXISTS `contract` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `contract_no` VARCHAR(64) NOT NULL COMMENT '合同编号',
    `party_a_name` VARCHAR(128) DEFAULT NULL COMMENT '甲方名称',
    `party_a_contact` VARCHAR(128) DEFAULT NULL COMMENT '甲方联系人',
    `party_a_phone` VARCHAR(256) DEFAULT NULL COMMENT '甲方联系电话',
    `party_a_id_card` VARCHAR(256) DEFAULT NULL COMMENT '甲方身份证号',
    `contract_amount` VARCHAR(256) DEFAULT NULL COMMENT '合同金额(加密存储)',
    `sign_date` DATE DEFAULT NULL COMMENT '签订日期',
    `expire_date` DATE DEFAULT NULL COMMENT '到期日期',
    `contract_status` VARCHAR(32) DEFAULT 'DRAFT' COMMENT '合同状态',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contract_no` (`contract_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同表';

-- 测试数据：customer
INSERT INTO `customer` (`customer_no`, `customer_name`, `id_card`, `phone`, `email`, `bank_card_no`, `address`) VALUES
('C20260001', '张三', '110101199001011234', '13800138001', 'zhangsan@example.com', '6222021234567890123', '北京市朝阳区建国路88号'),
('C20260002', '李四', '310101199203032345', '13900139002', 'lisi@example.com', '6222021234567890456', '上海市浦东新区陆家嘴路100号'),
('C20260003', '王五', '440101199505053456', '13700137003', 'wangwu@example.com', '6222021234567890789', '广州市天河区天河路200号'),
('C20260004', '赵六', '500101199807074567', '13600136004', 'zhaoliu@example.com', '6222021234567891011', '重庆市渝中区解放碑路300号'),
('C20260005', '孙七', '330101200009095678', '13500135005', 'sunqi@example.com', '6222021234567891345', '杭州市西湖区文三路400号');

-- 测试数据：employee
INSERT INTO `employee` (`emp_no`, `emp_name`, `id_card`, `phone`, `email`, `salary`, `bank_card_no`, `department`, `position`, `hire_date`) VALUES
('E20260001', '陈一', '110101199101011111', '13800138011', 'chenyi@company.com', '25000', '6222021111111111111', '技术部', '高级工程师', '2020-03-15'),
('E20260002', '周二', '310101199202022222', '13900139022', 'zhouer@company.com', '30000', '6222022222222222222', '产品部', '产品经理', '2019-07-01'),
('E20260003', '吴三', '440101199303033333', '13700137033', 'wusan@company.com', '18000', '6222023333333333333', '技术部', '工程师', '2021-09-20'),
('E20260004', '郑四', '500101199404044444', '13600136044', 'zhengsi@company.com', '45000', '6222024444444444444', '管理层', '总监', '2018-01-10'),
('E20260005', '王五', '330101199505055555', '13500135055', 'wangwu@company.com', '22000', '6222025555555555555', '市场部', '市场专员', '2022-05-08');

-- 测试数据：orders
INSERT INTO `orders` (`order_no`, `customer_id`, `receiver_name`, `receiver_phone`, `receiver_address`, `pay_account`, `total_amount`, `order_status`) VALUES
('ORD20260001', 1, '张三', '13800138001', '北京市朝阳区建国路88号', 'zhangsan@alipay.com', 1299.00, 'COMPLETED'),
('ORD20260002', 2, '李四', '13900139002', '上海市浦东新区陆家嘴路100号', 'lisi@wxpay.com', 3580.50, 'SHIPPED'),
('ORD20260003', 1, '张三', '13800138001', '北京市朝阳区建国路88号', 'zhangsan@alipay.com', 899.00, 'CREATED'),
('ORD20260004', 3, '王五', '13700137003', '广州市天河区天河路200号', 'wangwu@alipay.com', 12500.00, 'COMPLETED'),
('ORD20260005', 4, '赵六', '13600136004', '重庆市渝中区解放碑路300号', 'zhaoliu@wxpay.com', 6780.00, 'PAID');

-- 测试数据：contract
INSERT INTO `contract` (`contract_no`, `party_a_name`, `party_a_contact`, `party_a_phone`, `party_a_id_card`, `contract_amount`, `sign_date`, `expire_date`, `contract_status`) VALUES
('CT20260001', '北京XX科技有限公司', '刘经理', '13800138001', '110101198001011234', '500000', '2025-01-15', '2026-12-31', 'EFFECTIVE'),
('CT20260002', '上海YY贸易有限公司', '陈总', '13900139002', '310101198202022345', '1200000', '2025-03-01', '2027-02-28', 'EFFECTIVE'),
('CT20260003', '广州ZZ实业有限公司', '王总', '13700137003', '440101198303033456', '780000', '2025-06-10', '2026-06-09', 'EXPIRED'),
('CT20260004', '深圳AA投资有限公司', '赵总', '13600136004', '440301198404044567', '2500000', '2025-09-01', '2028-08-31', 'EFFECTIVE'),
('CT20260005', '杭州BB网络技术有限公司', '孙总', '13500135005', '330101198505055678', '360000', '2026-01-20', '2027-01-19', 'DRAFT');