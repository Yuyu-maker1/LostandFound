-- ============================================================
-- 文件: sql/20261103_add_admin_role.sql
-- 用途: 为管理端账号表 user1 增加角色字段，并预置超级管理员 admin / 123456
-- 说明: 脚本可重复执行（幂等），不会重复插入账号，也不会重复加列
-- 执行方式（必须显式指定目标库，脚本不再写死库名）:
--   mysql -h 127.0.0.1 -P 3306 -uroot -p lostandfound < sql/20261103_add_admin_role.sql
--   如果实际库名不是 lostandfound，把命令末尾的库名替换掉即可
-- 注意: 未对生产库自动执行，请确认后再手工运行
-- ============================================================

-- 显示当前操作库，执行后应输出 lostandfound（或你传入的库名），避免改错库
SELECT DATABASE() AS current_database;

-- 当前库若没有 user1 表，先按最终结构建表（已存在则跳过）
CREATE TABLE IF NOT EXISTS `user1` (
  `name`    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '登录账号',
  `pwd`     VARCHAR(50)  DEFAULT NULL COMMENT 'MD5 密文密码（32 位小写）',
  `userimg` VARCHAR(255) DEFAULT NULL COMMENT '头像地址，管理端已停用，保留给旅客端',
  `role`    VARCHAR(20)  NOT NULL DEFAULT 'admin' COMMENT '角色：super_admin / admin',
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci COMMENT='管理员表';

-- 老库补列：MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，用 information_schema 判断
SET @role_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'user1'
    AND column_name = 'role'
);

SET @ddl = IF(
  @role_exists = 0,
  'ALTER TABLE `user1` ADD COLUMN `role` VARCHAR(20) NOT NULL DEFAULT ''admin'' COMMENT ''角色：super_admin / admin'' AFTER `userimg`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 预置超级管理员：admin / 123456
-- MD5("123456") = e10adc3949ba59abbe56e057f20f883e
INSERT INTO `user1` (`name`, `pwd`, `role`)
SELECT 'admin', 'e10adc3949ba59abbe56e057f20f883e', 'super_admin'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `user1` WHERE `name` = 'admin');

-- 已存在的 admin 账号统一提升为超级管理员（重复执行无副作用）
UPDATE `user1` SET `role` = 'super_admin' WHERE `name` = 'admin';

-- 其余历史账号统一为普通管理员（NULL 或空值兜底）
UPDATE `user1` SET `role` = 'admin' WHERE `role` IS NULL OR `role` = '';

-- 校验结果
SELECT `name`, `role`, `pwd` FROM `user1` ORDER BY `name`;
