-- ============================================================
-- 文件: sql/20261104_add_admin_profile_and_station.sql
-- 用途: 1) 新建/纠正车站字典 station 并预置常见车站
--       2) 为管理员表 user1 补齐员工资料字段（工号/车站/姓名/手机号/身份证/备注/状态）
-- 说明: 幂等脚本，可重复执行；不写死库名，执行前必须已选择库（-D 或 USE）
-- 执行(PowerShell):
--   & "D:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --default-character-set=utf8mb4 `
--     -h "127.0.0.1" -P "3306" -uroot -p -D lostandfound `
--     -e "source E:/Users/Lenovo/documents/Trae_files/LostandFound/sql/20261104_add_admin_profile_and_station.sql"
-- 注意: 未自动对生产库执行；执行前请备份
-- 修订(2026-11-04): 原脚本在客户端默认字符集为 gbk 时，本文件(UTF-8)里的中文会被按 GBK 错拆，
--   相邻行的单引号被当作双字节字符的一部分吞掉，多行数据被拼成一条超长字符串，
--   触发 ERROR 1406 (22001) Data too long for column 'station_name'。
--   现改为脚本内强制 SET NAMES utf8mb4，并把 station_name / city 放宽为 VARCHAR(64) utf8mb4，
--   同时对已存在的旧表执行 MODIFY COLUMN 纠正，保证“已部分执行”的库也能直接重跑。
-- ============================================================

SELECT DATABASE() AS current_database;

-- 关键：不依赖客户端默认字符集（中文 Windows 下常为 gbk），统一按 UTF-8 解析本文件
SET NAMES utf8mb4;

-- ============ 一、车站字典 ============
CREATE TABLE IF NOT EXISTS `station` (
  `id`           INT          NOT NULL AUTO_INCREMENT COMMENT '车站ID',
  `station_name` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '车站名称',
  `city`         VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属城市',
  `is_enabled`   TINYINT      NOT NULL DEFAULT 1 COMMENT '1-启用 0-停用',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_station_name` (`station_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车站字典';

-- 若 station 表已由旧脚本/手工创建（列过小或为 utf8mb3），先纠正列定义，再插入数据
SET @c = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'station' AND column_name = 'station_name');
SET @ddl = IF(@c > 0,
  'ALTER TABLE `station` MODIFY COLUMN `station_name` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT ''车站名称''',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'station' AND column_name = 'city');
SET @ddl = IF(@c > 0,
  'ALTER TABLE `station` MODIFY COLUMN `city` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''所属城市''',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'station') > 0,
  'ALTER TABLE `station` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 旧表若缺唯一索引则补建（已有则跳过，避免 Duplicate key name）
SET @i = (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'station' AND index_name = 'uk_station_name');
SET @ddl = IF(@i = 0, 'ALTER TABLE `station` ADD UNIQUE KEY `uk_station_name` (`station_name`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 预置常见车站（唯一键冲突时执行空更新，可重复执行）
INSERT INTO `station` (`station_name`, `city`) VALUES
  ('北京南', '北京'), ('北京西', '北京'), ('上海虹桥', '上海'), ('上海站', '上海'),
  ('广州南', '广州'), ('深圳北', '深圳'), ('杭州东', '杭州'), ('南京南', '南京'),
  ('武汉', '武汉'), ('成都东', '成都'), ('西安北', '西安'), ('郑州东', '郑州'),
  ('长沙南', '长沙'), ('天津西', '天津'), ('重庆北', '重庆')
ON DUPLICATE KEY UPDATE `station_name` = `station_name`;

-- ============ 二、user1 补 role（兼容旧脚本未执行的情况） ============
SET @role_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'user1' AND column_name = 'role'
);
SET @ddl = IF(@role_exists = 0,
  'ALTER TABLE `user1` ADD COLUMN `role` VARCHAR(20) NOT NULL DEFAULT ''admin'' COMMENT ''角色：super_admin / admin''',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============ 三、user1 补员工资料字段（全部允许 NULL，保证旧账号不受影响） ============
SET @c = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'user1' AND column_name = 'staff_no');
SET @ddl = IF(@c = 0,
  'ALTER TABLE `user1` ADD COLUMN `staff_no` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''员工工号''',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'user1' AND column_name = 'station_id');
SET @ddl = IF(@c = 0, 'ALTER TABLE `user1` ADD COLUMN `station_id` INT DEFAULT NULL COMMENT ''所属车站ID''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'user1' AND column_name = 'real_name');
SET @ddl = IF(@c = 0,
  'ALTER TABLE `user1` ADD COLUMN `real_name` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''真实姓名''',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'user1' AND column_name = 'phone');
SET @ddl = IF(@c = 0,
  'ALTER TABLE `user1` ADD COLUMN `phone` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''手机号''',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'user1' AND column_name = 'id_card');
SET @ddl = IF(@c = 0,
  'ALTER TABLE `user1` ADD COLUMN `id_card` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''身份证号（选填，配合 role 唯一）''',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'user1' AND column_name = 'remark');
SET @ddl = IF(@c = 0,
  'ALTER TABLE `user1` ADD COLUMN `remark` VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''备注（最长200字）''',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'user1' AND column_name = 'status');
SET @ddl = IF(@c = 0, 'ALTER TABLE `user1` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT ''1-启用 0-停用''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============ 四、唯一索引（先判重，重复执行不报错） ============
SET @i = (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'user1' AND index_name = 'uk_user1_staff_no');
SET @ddl = IF(@i = 0, 'ALTER TABLE `user1` ADD UNIQUE KEY `uk_user1_staff_no` (`staff_no`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @i = (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'user1' AND index_name = 'uk_user1_phone');
SET @ddl = IF(@i = 0, 'ALTER TABLE `user1` ADD UNIQUE KEY `uk_user1_phone` (`phone`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @i = (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'user1' AND index_name = 'uk_user1_role_id_card');
SET @ddl = IF(@i = 0, 'ALTER TABLE `user1` ADD UNIQUE KEY `uk_user1_role_id_card` (`role`, `id_card`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @i = (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'user1' AND index_name = 'idx_user1_station');
SET @ddl = IF(@i = 0, 'ALTER TABLE `user1` ADD KEY `idx_user1_station` (`station_id`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============ 五、预置超级管理员 admin / 123456 ============
INSERT INTO `user1` (`name`, `pwd`, `role`, `status`)
SELECT 'admin', 'e10adc3949ba59abbe56e057f20f883e', 'super_admin', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `user1` WHERE `name` = 'admin');

UPDATE `user1` SET `role` = 'super_admin' WHERE `name` = 'admin';
UPDATE `user1` SET `role` = 'admin' WHERE `role` IS NULL OR `role` = '';
UPDATE `user1` SET `status` = 1 WHERE `status` IS NULL;

-- ============ 六、结果校验 ============
SHOW COLUMNS FROM `station`;
SELECT COUNT(*) AS station_count FROM `station`;
SELECT `name`, `role`, `status`, `staff_no`, `station_id`, `real_name`, `phone`
FROM `user1` ORDER BY `name`;
