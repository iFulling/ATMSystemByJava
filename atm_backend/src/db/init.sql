-- 创建数据库
CREATE DATABASE IF NOT EXISTS db_atm;
USE db_atm;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    balance DECIMAL(10,2) DEFAULT 0.00,
    permissions_flags INT DEFAULT 15
);

-- 管理员表
CREATE TABLE IF NOT EXISTS admins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE
);

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    operation TEXT NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 转账记录表
CREATE TABLE IF NOT EXISTS transfer_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_user_id BIGINT,
    to_user_id BIGINT,
    amount DECIMAL(10,2) NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(255),
    status ENUM('SUCCESS', 'FAILED', 'PENDING') DEFAULT 'PENDING',
    permissions_snapshot INT,
    log_id BIGINT,
    FOREIGN KEY (from_user_id) REFERENCES users(id),
    FOREIGN KEY (to_user_id) REFERENCES users(id),
    FOREIGN KEY (log_id) REFERENCES operation_logs(id)
);

-- 创建默认管理员账户
INSERT INTO admins (username, password, enabled) 
VALUES ('fulling', 'vZM2IUp8cJBVpZbu:x/7Wg42eVn8r3CNQ8pdDiJTBxGBaYZxHzr16lyAuOyk=', true)
ON DUPLICATE KEY UPDATE username = username; 