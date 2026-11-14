USE highstream;

-- 用户表：归 user-service 独占
CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  avatar VARCHAR(255) NOT NULL DEFAULT '',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 预置两个用户，方便直接测试
INSERT INTO users(name, avatar) VALUES ('alice', ''), ('bob', '');
