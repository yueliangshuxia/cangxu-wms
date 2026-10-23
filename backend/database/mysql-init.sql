-- Cangxu WMS MySQL bootstrap (run once as root)
-- Usage: mysql -u root -p < backend/database/mysql-init.sql
--
-- Creates:
--   database : cangxu_wms (utf8mb4)
--   user     : wms / wms2026  (demo only; change before real use)
--
-- Tables use IF NOT EXISTS; seed rows use INSERT IGNORE. Rows in tables without a
-- unique key (inventory, audit_log, detail tables) will be appended if re-run.
-- generated from backend/src/main/resources/schema.sql + data.sql

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS cangxu_wms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'wms'@'localhost' IDENTIFIED BY 'wms2026';
GRANT ALL PRIVILEGES ON cangxu_wms.* TO 'wms'@'localhost';
FLUSH PRIVILEGES;
USE cangxu_wms;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    spec VARCHAR(128),
    unit VARCHAR(16) NOT NULL DEFAULT '件',
    temp_type VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    safety_stock INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ON_SALE',
    image_url VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS supplier (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    contact VARCHAR(64),
    phone VARCHAR(32),
    category VARCHAR(64),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    channel VARCHAR(32),
    contact VARCHAR(64),
    phone VARCHAR(32),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS warehouse (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    address VARCHAR(255),
    timezone VARCHAR(64) DEFAULT 'Asia/Shanghai'
);

CREATE TABLE IF NOT EXISTS zone (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_id BIGINT NOT NULL,
    code VARCHAR(16) NOT NULL,
    name VARCHAR(64) NOT NULL,
    temp_type VARCHAR(16) NOT NULL DEFAULT 'NORMAL'
);

CREATE TABLE IF NOT EXISTS location (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    zone_id BIGINT NOT NULL,
    code VARCHAR(32) NOT NULL UNIQUE,
    row_no INT NOT NULL,
    col_no INT NOT NULL,
    capacity INT NOT NULL DEFAULT 120,
    status VARCHAR(16) NOT NULL DEFAULT 'FREE',
    current_sku VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    location_id BIGINT,
    qty INT NOT NULL DEFAULT 0,
    locked_qty INT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64),
    role_code VARCHAR(32),
    module VARCHAR(32) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target VARCHAR(255),
    ip VARCHAR(64),
    result VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
    detail VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS inbound_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    no VARCHAR(64) NOT NULL UNIQUE,
    supplier_name VARCHAR(128),
    dock VARCHAR(32),
    eta DATETIME,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    operator VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inbound_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT,
    sku VARCHAR(64),
    product_name VARCHAR(128),
    qty INT NOT NULL,
    location_id BIGINT,
    location_code VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS outbound_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    no VARCHAR(64) NOT NULL UNIQUE,
    customer_name VARCHAR(128),
    priority VARCHAR(16) DEFAULT 'NORMAL',
    wave_no VARCHAR(32),
    status VARCHAR(16) DEFAULT 'PENDING',
    item_count INT DEFAULT 0,
    total_qty INT DEFAULT 0,
    promised_at VARCHAR(32),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS outbound_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT,
    sku VARCHAR(64),
    product_name VARCHAR(128),
    qty INT NOT NULL,
    location_id BIGINT,
    location_code VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS device (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    type VARCHAR(32),
    location VARCHAR(128),
    status VARCHAR(16) DEFAULT 'ONLINE',
    battery INT,
    signal_strength VARCHAR(32),
    last_heartbeat DATETIME
);

CREATE TABLE IF NOT EXISTS device_telemetry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_code VARCHAR(64) NOT NULL,
    metric VARCHAR(32) NOT NULL,
    metric_value DECIMAL(10,2) NOT NULL,
    unit VARCHAR(16),
    collected_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS stocktake_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    no VARCHAR(64) NOT NULL UNIQUE,
    scope VARCHAR(128),
    type VARCHAR(32),
    owner VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    total INT NOT NULL DEFAULT 0,
    counted INT NOT NULL DEFAULT 0,
    diff_count INT NOT NULL DEFAULT 0,
    started_at VARCHAR(32),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stocktake_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    sku VARCHAR(64),
    product_name VARCHAR(128),
    location_code VARCHAR(32),
    book_qty INT NOT NULL DEFAULT 0,
    actual_qty INT NOT NULL DEFAULT 0,
    diff_qty INT NOT NULL DEFAULT 0,
    reason VARCHAR(128),
    handle_action VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS return_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    no VARCHAR(64) NOT NULL UNIQUE,
    customer_name VARCHAR(128),
    product_name VARCHAR(128),
    sku VARCHAR(64),
    qty INT NOT NULL DEFAULT 0,
    reason VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_INSPECTION',
    applied_at VARCHAR(32),
    disposition VARCHAR(16),
    note VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS system_setting (
    setting_key VARCHAR(64) PRIMARY KEY,
    setting_value VARCHAR(500),
    group_name VARCHAR(32)
);

-- Seed data
-- Demo seed data. Password hashes are BCrypt.
-- chenyu / wms2026, admin / admin123, linzhou / wms2026, zhouke / wms2026
INSERT IGNORE INTO sys_user (username, password_hash, real_name, role_code, status) VALUES
('admin', '$2b$10$HgntJ3y03f.JKk8tvZaisejtmU.pTsFAfXmaCDUgMGoPrvQf8gGk.', '系统管理员', 'ADMIN', 'ACTIVE'),
('chenyu', '$2b$10$tXcgvfWf3lJ2wV2ax494ZO5mZqHwLgaAycErM.VXmD3k6KjuaGiea', '陈屿', 'MANAGER', 'ACTIVE'),
('linzhou', '$2b$10$tXcgvfWf3lJ2wV2ax494ZO5mZqHwLgaAycErM.VXmD3k6KjuaGiea', '林舟', 'RECEIVER', 'ACTIVE'),
('zhouke', '$2b$10$tXcgvfWf3lJ2wV2ax494ZO5mZqHwLgaAycErM.VXmD3k6KjuaGiea', '周可', 'INSPECTOR', 'ACTIVE');

INSERT IGNORE INTO product (sku, name, category, spec, unit, temp_type, safety_stock, status, image_url) VALUES
('WH-1001', '降噪蓝牙耳机', '电子配件', '入耳式 / 蓝牙 5.3', '件', 'NORMAL', 300, 'ON_SALE', '/images/products/headphone.jpg'),
('WH-1002', '机械键盘 K87', '电子配件', '87 键 / 青轴', '件', 'NORMAL', 200, 'ON_SALE', '/images/products/keyboard.jpg'),
('WH-1003', '20000mAh 充电宝', '电子配件', '20000mAh / 22.5W', '件', 'NORMAL', 120, 'ON_SALE', '/images/products/powerbank.jpg'),
('WH-1004', '无线静音鼠标', '电子配件', '2.4G / 静音微动', '件', 'NORMAL', 200, 'ON_SALE', '/images/products/mouse.jpg'),
('WH-1005', '智能运动手表', '电子配件', '46mm / GPS', '件', 'NORMAL', 80, 'ON_SALE', '/images/products/watch.jpg'),
('WH-1006', '65W 氮化镓充电器', '电子配件', '65W / 双 C 口', '件', 'NORMAL', 100, 'ON_SALE', '/images/products/charger.jpg'),
('WH-1007', 'USB-C 编织数据线', '电子配件', '1.5m / 100W', '件', 'NORMAL', 500, 'ON_SALE', '/images/products/cable.jpg'),
('WH-2001', '护眼台灯 Pro', '智能家居', '国 AA / 无频闪', '台', 'NORMAL', 150, 'ON_SALE', '/images/products/lamp.jpg'),
('WH-2002', '智能音箱 Mini', '智能家居', '5W / WiFi', '台', 'NORMAL', 100, 'ON_SALE', '/images/products/speaker.jpg'),
('WH-3001', '城市通勤双肩包', '户外装备', '25L / 防泼水', '件', 'NORMAL', 250, 'ON_SALE', '/images/products/backpack.jpg'),
('WH-3002', '不锈钢保温杯 500ml', '生活用品', '316L / 500ml', '个', 'NORMAL', 400, 'ON_SALE', '/images/products/bottle.jpg'),
('WH-2003', '七合一扩展坞', '电子配件', '7 合 1 / HDMI', '件', 'NORMAL', 100, 'ON_SALE', '/images/products/hub.jpg');

INSERT IGNORE INTO supplier (code, name, contact, phone, category, status) VALUES
('SUP-001', '深圳华创电子', '李工', '138****6210', '电子配件', 'ACTIVE'),
('SUP-002', '东莞智造科技', '陈工', '139****3382', '智能家居', 'ACTIVE'),
('SUP-003', '苏州恒达物流', '周经理', '137****8821', '日用百货', 'ACTIVE'),
('SUP-004', '杭州云集供应链', '王经理', '135****1076', '户外装备', 'ACTIVE'),
('SUP-005', '宁波海川日用品', '徐经理', '136****4420', '生活用品', 'ACTIVE'),
('SUP-006', '上海蓝芯半导体', '赵工', '133****7712', '电子元件', 'EVALUATING'),
('SUP-007', '无锡捷运包装', '钱经理', '132****9088', '包装耗材', 'ACTIVE'),
('SUP-008', '南京云仓科技', '孙工', '131****5566', '仓储服务', 'SUSPENDED');

INSERT IGNORE INTO customer (code, name, channel, contact, phone, status) VALUES
('CUS-001', '天猫旗舰店', '电商', '王经理', '138****1024', 'ACTIVE'),
('CUS-002', '京东自营', '电商', '李经理', '139****2088', 'ACTIVE'),
('CUS-003', '抖音商城', '电商', '周经理', '137****3301', 'ACTIVE'),
('CUS-004', '拼多多官方店', '电商', '陈经理', '136****4420', 'ACTIVE'),
('CUS-005', '线下门店 · 苏州中心', '线下', '许店长', '135****7712', 'ACTIVE'),
('CUS-006', '线下门店 · 园区店', '线下', '赵店长', '134****9088', 'ACTIVE'),
('CUS-007', '企业采购 · 恒远科技', 'B2B', '钱经理', '133****5566', 'ACTIVE'),
('CUS-008', '企业采购 · 启明制造', 'B2B', '孙经理', '132****1199', 'SUSPENDED');

INSERT IGNORE INTO warehouse (code, name, address, timezone) VALUES
('WMS-SZ-001', '华东一号仓', '江苏省苏州市工业园区物流大道 88 号', 'Asia/Shanghai');

INSERT IGNORE INTO zone (warehouse_id, code, name, temp_type) VALUES
(1, 'A', '电子配件', 'NORMAL'),
(1, 'B', '智能家居', 'NORMAL'),
(1, 'C', '户外与生活', 'NORMAL'),
(1, 'D', '大件商品', 'CONSTANT'),
(1, 'E', '退货暂存', 'NORMAL');

INSERT IGNORE INTO location (zone_id, code, row_no, col_no, capacity, status, current_sku) VALUES
(1, 'A-01-03', 1, 3, 120, 'OCCUPIED', 'WH-1001'),
(1, 'A-02-01', 2, 1, 120, 'OCCUPIED', 'WH-1003'),
(1, 'A-01-10', 1, 10, 120, 'FREE', NULL),
(2, 'B-01-02', 1, 2, 120, 'OCCUPIED', 'WH-2001'),
(2, 'B-01-05', 1, 5, 120, 'FREE', NULL),
(3, 'C-03-01', 3, 1, 120, 'OCCUPIED', 'WH-3002'),
(4, 'D-02-01', 2, 1, 60, 'FREE', NULL),
(5, 'E-01-01', 1, 1, 120, 'FREE', NULL);

INSERT IGNORE INTO inventory (product_id, location_id, qty, locked_qty) VALUES
(1, 1, 1280, 190),
(2, NULL, 640, 30),
(3, 2, 86, 26),
(4, NULL, 940, 20),
(5, NULL, 220, 70),
(6, NULL, 58, 18),
(7, NULL, 2400, 140),
(8, 4, 420, 40),
(9, NULL, 310, 20),
(10, NULL, 780, 60),
(11, 6, 1560, 60),
(12, NULL, 340, 40);

INSERT IGNORE INTO audit_log (username, role_code, module, action, target, ip, result, detail) VALUES
('chenyu', 'MANAGER', 'SYSTEM', '登录系统', 'sys_user:chenyu', '127.0.0.1', 'SUCCESS', '演示登录'),
('linzhou', 'RECEIVER', 'INBOUND', '上架确认', 'ASN-20261020-004 / A-01-10', '127.0.0.1', 'SUCCESS', '推荐库位 A-01-10'),
('zhouke', 'INSPECTOR', 'INVENTORY', '库存调整', 'WH-1005 / -2', '127.0.0.1', 'SUCCESS', '盘点差异调整');

INSERT IGNORE INTO inbound_order (id, no, supplier_name, dock, eta, status, operator, created_at) VALUES
(1, 'ASN-20261020-001', '深圳华创电子', '1 号月台', '2026-10-20 09:30:00', 'PENDING', '林舟', TIMESTAMPADD(DAY, -0, CURRENT_TIMESTAMP)),
(2, 'ASN-20261020-002', '东莞智造科技', '1 号月台', '2026-10-20 10:10:00', 'PENDING', '林舟', TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP)),
(3, 'ASN-20261020-003', '深圳华创电子', '3 号月台', '2026-10-20 10:35:00', 'INSPECTING', '周可', TIMESTAMPADD(DAY, -2, CURRENT_TIMESTAMP)),
(4, 'ASN-20261020-004', '苏州恒达物流', '2 号月台', '2026-10-20 11:00:00', 'READY', '陈屿', TIMESTAMPADD(DAY, -3, CURRENT_TIMESTAMP)),
(5, 'ASN-20261020-005', '杭州云集供应链', '4 号月台', '2026-10-20 11:20:00', 'PENDING', '林舟', TIMESTAMPADD(DAY, -4, CURRENT_TIMESTAMP)),
(6, 'ASN-20261020-006', '宁波海川日用品', '1 号月台', '2026-10-20 11:45:00', 'READY', '周可', TIMESTAMPADD(DAY, -5, CURRENT_TIMESTAMP)),
(7, 'ASN-20261020-007', '东莞智造科技', '2 号月台', '2026-10-20 12:10:00', 'INSPECTING', '陈屿', TIMESTAMPADD(DAY, -6, CURRENT_TIMESTAMP)),
(8, 'ASN-20261020-008', '深圳华创电子', '3 号月台', '2026-10-20 13:00:00', 'PENDING', '林舟', TIMESTAMPADD(DAY, -8, CURRENT_TIMESTAMP)),
(9, 'ASN-20261020-009', '杭州云集供应链', '4 号月台', '2026-10-20 13:30:00', 'READY', '周可', TIMESTAMPADD(DAY, -10, CURRENT_TIMESTAMP)),
(10, 'ASN-20261020-010', '宁波海川日用品', '1 号月台', '2026-10-20 14:00:00', 'PENDING', '陈屿', TIMESTAMPADD(DAY, -12, CURRENT_TIMESTAMP)),
(11, 'ASN-20261019-012', '深圳华创电子', '1 号月台', '2026-10-19 16:20:00', 'DONE', '周可', TIMESTAMPADD(DAY, -15, CURRENT_TIMESTAMP)),
(12, 'ASN-20261019-013', '苏州恒达物流', '3 号月台', '2026-10-19 15:40:00', 'DONE', '陈屿', TIMESTAMPADD(DAY, -18, CURRENT_TIMESTAMP)),
(13, 'ASN-20261019-014', '杭州云集供应链', '2 号月台', '2026-10-19 14:10:00', 'DONE', '林舟', TIMESTAMPADD(DAY, -21, CURRENT_TIMESTAMP));

INSERT IGNORE INTO inbound_item (order_id, product_id, sku, product_name, qty) VALUES
(1, 1, 'WH-1001', '降噪蓝牙耳机', 240),
(2, 2, 'WH-1002', '机械键盘 K87', 160),
(3, 6, 'WH-1006', '65W 氮化镓充电器', 180),
(4, 8, 'WH-2001', '护眼台灯 Pro', 120),
(5, 10, 'WH-3001', '城市通勤双肩包', 300),
(6, 11, 'WH-3002', '不锈钢保温杯 500ml', 480),
(7, 4, 'WH-1004', '无线静音鼠标', 260),
(8, 5, 'WH-1005', '智能运动手表', 90),
(9, 9, 'WH-2002', '智能音箱 Mini', 140),
(10, 7, 'WH-1007', 'USB-C 编织数据线', 600),
(11, 5, 'WH-1005', '智能运动手表', 60),
(12, 8, 'WH-2001', '护眼台灯 Pro', 80),
(13, 10, 'WH-3001', '城市通勤双肩包', 200);

INSERT IGNORE INTO outbound_order (id, no, customer_name, priority, wave_no, status, item_count, total_qty, promised_at, created_at) VALUES
(1, 'SO-20261020-018', '天猫旗舰店', 'URGENT', 'W-07', 'PICKING', 3, 72, '11:30', TIMESTAMPADD(DAY, -0, CURRENT_TIMESTAMP)),
(2, 'SO-20261020-017', '京东自营', 'NORMAL', 'W-07', 'PICKING', 3, 64, '12:00', TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP)),
(3, 'SO-20261020-016', '抖音商城', 'URGENT', 'W-06', 'REVIEW', 3, 80, '10:50', TIMESTAMPADD(DAY, -2, CURRENT_TIMESTAMP)),
(4, 'SO-20261020-015', '线下门店 · 苏州中心', 'NORMAL', 'W-06', 'REVIEW', 4, 210, '11:10', TIMESTAMPADD(DAY, -3, CURRENT_TIMESTAMP)),
(5, 'SO-20261020-014', '天猫旗舰店', 'NORMAL', 'W-05', 'SHIPPED', 2, 24, '09:40', TIMESTAMPADD(DAY, -4, CURRENT_TIMESTAMP)),
(6, 'SO-20261020-013', '拼多多官方店', 'NORMAL', 'W-05', 'SHIPPED', 3, 128, '09:20', TIMESTAMPADD(DAY, -5, CURRENT_TIMESTAMP)),
(7, 'SO-20261020-012', '京东自营', 'URGENT', 'W-04', 'SHIPPED', 2, 42, '08:55', TIMESTAMPADD(DAY, -6, CURRENT_TIMESTAMP)),
(8, 'SO-20261020-011', '抖音商城', 'NORMAL', NULL, 'PENDING', 3, 96, '13:00', TIMESTAMPADD(DAY, -7, CURRENT_TIMESTAMP)),
(9, 'SO-20261020-010', '线下门店 · 园区店', 'NORMAL', NULL, 'PENDING', 2, 38, '13:30', TIMESTAMPADD(DAY, -9, CURRENT_TIMESTAMP)),
(10, 'SO-20261020-009', '天猫旗舰店', 'URGENT', NULL, 'PENDING', 2, 56, '13:40', TIMESTAMPADD(DAY, -12, CURRENT_TIMESTAMP));

INSERT IGNORE INTO outbound_item (order_id, product_id, sku, product_name, qty, location_code) VALUES
(1, 1, 'WH-1001', '降噪蓝牙耳机', 36, 'A-01-03'),
(1, 4, 'WH-1004', '无线静音鼠标', 24, 'A-02-04'),
(1, 8, 'WH-2001', '护眼台灯 Pro', 12, 'B-01-02'),
(2, 9, 'WH-2002', '智能音箱 Mini', 18, 'B-01-06'),
(2, 10, 'WH-3001', '城市通勤双肩包', 20, 'C-02-03'),
(2, 7, 'WH-1007', 'USB-C 编织数据线', 26, 'A-03-05'),
(3, 2, 'WH-1002', '机械键盘 K87', 30, 'A-01-06'),
(3, 3, 'WH-1003', '20000mAh 充电宝', 20, 'A-02-01'),
(3, 8, 'WH-2001', '护眼台灯 Pro', 30, 'B-01-02'),
(4, 11, 'WH-3002', '不锈钢保温杯 500ml', 80, 'C-03-01'),
(4, 10, 'WH-3001', '城市通勤双肩包', 60, 'C-02-03'),
(4, 9, 'WH-2002', '智能音箱 Mini', 40, 'B-01-06'),
(4, 1, 'WH-1001', '降噪蓝牙耳机', 30, 'A-01-03'),
(5, 1, 'WH-1001', '降噪蓝牙耳机', 12, 'A-01-03'),
(5, 7, 'WH-1007', 'USB-C 编织数据线', 12, 'A-03-05'),
(6, 2, 'WH-1002', '机械键盘 K87', 50, 'A-01-06'),
(6, 4, 'WH-1004', '无线静音鼠标', 48, 'A-02-04'),
(6, 9, 'WH-2002', '智能音箱 Mini', 30, 'B-01-06'),
(7, 3, 'WH-1003', '20000mAh 充电宝', 20, 'A-02-01'),
(7, 5, 'WH-1005', '智能运动手表', 22, 'A-03-08'),
(8, 1, 'WH-1001', '降噪蓝牙耳机', 40, 'A-01-03'),
(8, 8, 'WH-2001', '护眼台灯 Pro', 30, 'B-01-02'),
(8, 11, 'WH-3002', '不锈钢保温杯 500ml', 26, 'C-03-01'),
(9, 4, 'WH-1004', '无线静音鼠标', 20, 'A-02-04'),
(9, 7, 'WH-1007', 'USB-C 编织数据线', 18, 'A-03-05'),
(10, 9, 'WH-2002', '智能音箱 Mini', 30, 'B-01-06'),
(10, 10, 'WH-3001', '城市通勤双肩包', 26, 'C-02-03');

INSERT IGNORE INTO device (code, type, location, status, battery, signal_strength, last_heartbeat) VALUES
('GW-EDGE-01', 'EDGE_GATEWAY', '机房 · 1F', 'ONLINE', NULL, '-42 dBm', CURRENT_TIMESTAMP),
('TH-A01', 'TEMP_HUMIDITY', 'A 区 · 03 巷道', 'ONLINE', 86, '-51 dBm', CURRENT_TIMESTAMP),
('TH-B01', 'TEMP_HUMIDITY', 'B 区 · 01 巷道', 'ONLINE', 74, '-58 dBm', CURRENT_TIMESTAMP),
('TH-D01', 'TEMP_HUMIDITY', 'D 区 · 02 巷道', 'ONLINE', 91, '-49 dBm', CURRENT_TIMESTAMP),
('SM-A01', 'SMOKE', 'A 区 · 顶部', 'ONLINE', 98, '-60 dBm', CURRENT_TIMESTAMP),
('DR-MAIN', 'DOOR_ACCESS', '主入口', 'ONLINE', NULL, '有线', CURRENT_TIMESTAMP),
('RFID-A01', 'RFID_READER', 'A 区 · 收货口', 'ONLINE', NULL, '有线', CURRENT_TIMESTAMP),
('AGV-01', 'AGV', 'A 区 · 主通道', 'WORKING', 72, 'WiFi -47 dBm', CURRENT_TIMESTAMP),
('AGV-04', 'AGV', 'B 区 · 主通道', 'WORKING', 64, 'WiFi -53 dBm', CURRENT_TIMESTAMP),
('AGV-07', 'AGV', '充电位 C-00-02', 'CHARGING', 18, 'WiFi -61 dBm', CURRENT_TIMESTAMP);

INSERT IGNORE INTO stocktake_task (id, no, scope, type, owner, status, total, counted, diff_count, started_at) VALUES
(1, 'CC-20261020-A02', 'A 区 · 02 巷道', '循环盘点', '周可', 'IN_PROGRESS', 120, 82, 2, '10-20 09:20'),
(2, 'CC-20261020-B01', 'B 区 · 01 巷道', '循环盘点', '陈屿', 'IN_PROGRESS', 120, 50, 0, '10-20 10:10'),
(3, 'CC-20261020-D01', 'D 区 · 大件区', '抽盘', '林舟', 'PENDING', 60, 0, 0, '10-20 14:00'),
(4, 'CC-20261019-A01', 'A 区 · 01 巷道', '循环盘点', '周可', 'DONE', 120, 120, 0, '10-19 09:00'),
(5, 'CC-20261019-C03', 'C 区 · 03 巷道', '循环盘点', '陈屿', 'DONE', 120, 120, 4, '10-19 13:30'),
(6, 'CC-20261018-ALL', '全仓 · A–E 区', '全盘', '陈屿', 'DONE', 1280, 1280, 12, '10-18 08:30'),
(7, 'CC-20261017-E01', 'E 区 · 退货暂存', '抽盘', '林舟', 'DONE', 40, 40, 1, '10-17 15:00'),
(8, 'CC-20261021-B02', 'B 区 · 02 巷道', '循环盘点', '周可', 'PENDING', 120, 0, 0, '10-21 09:00');

INSERT IGNORE INTO stocktake_item (task_id, sku, product_name, location_code, book_qty, actual_qty, diff_qty, reason, handle_action) VALUES
(1, 'WH-1005', '智能运动手表', 'A-03-02', 220, 218, -2, '拣货未及时扣减', 'ADJUST'),
(1, 'WH-1007', 'USB-C 编织数据线', 'A-04-01', 2400, 2401, 1, '上架重复扫描', 'REVIEWED'),
(5, 'WH-3002', '保温杯 500ml', 'C-03-01', 1560, 1558, -2, '包装破损报损', 'ADJUST'),
(5, 'WH-2002', '智能音箱 Mini', 'B-01-06', 310, 308, -2, '待检区未隔离', 'ADJUST'),
(5, 'WH-1003', '充电宝', 'A-02-01', 86, 89, 3, '退货未入账', 'REVIEWED');

INSERT IGNORE INTO return_order (id, no, customer_name, product_name, sku, qty, reason, status, applied_at, disposition) VALUES
(1, 'RT-20261020-006', '天猫旗舰店', '降噪蓝牙耳机', 'WH-1001', 6, '七天无理由', 'PENDING_INSPECTION', '10-20 10:12', NULL),
(2, 'RT-20261020-005', '京东自营', '机械键盘 K87', 'WH-1002', 3, '质量问题', 'PENDING_INSPECTION', '10-20 09:48', NULL),
(3, 'RT-20261020-004', '抖音商城', '智能音箱 Mini', 'WH-2002', 2, '包装破损', 'READY_TO_PUTAWAY', '10-20 09:20', 'resell'),
(4, 'RT-20261020-003', '线下门店 · 苏州中心', '护眼台灯 Pro', 'WH-2001', 1, '发错货', 'READY_TO_PUTAWAY', '10-20 08:55', 'resell'),
(5, 'RT-20261020-002', '拼多多官方店', 'USB-C 编织数据线', 'WH-1007', 12, '七天无理由', 'IN_STOCK', '10-19 16:40', 'repair'),
(6, 'RT-20261020-001', '天猫旗舰店', '城市通勤双肩包', 'WH-3001', 4, '质量问题', 'IN_STOCK', '10-19 15:30', 'resell'),
(7, 'RT-20261019-008', '京东自营', '20000mAh 充电宝', 'WH-1003', 2, '包装破损', 'SCRAPPED', '10-19 14:10', 'scrap'),
(8, 'RT-20261019-007', '抖音商城', '无线静音鼠标', 'WH-1004', 5, '七天无理由', 'IN_STOCK', '10-19 11:25', 'resell');

INSERT IGNORE INTO system_setting (setting_key, setting_value, group_name) VALUES
('warehouse.name', '华东一号仓', 'warehouse'),
('warehouse.code', 'WMS-SZ-001', 'warehouse'),
('warehouse.address', '江苏省苏州市工业园区物流大道 88 号', 'warehouse'),
('warehouse.timezone', 'Asia/Shanghai (UTC+8)', 'warehouse'),
('warehouse.hours', '08:00 – 20:00', 'warehouse'),
('warehouse.temp', '常温 15–28°C · 恒温 18–24°C', 'warehouse'),
('warehouse.capacity', '1280', 'warehouse'),
('alert.lowStock', 'true', 'alert'),
('alert.temp', 'true', 'alert'),
('alert.deviceOffline', 'true', 'alert'),
('alert.pickTimeout', 'false', 'alert'),
('notify.site', 'true', 'notify'),
('notify.email', 'true', 'notify'),
('notify.wecom', 'false', 'notify'),
('notify.sound', 'false', 'notify');
