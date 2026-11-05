-- ============================================================
-- 仅测试用：新增/重置旅客端测试账号 testuser / 123456
-- 执行前请确认连接的是开发库，不要在生产库执行
-- 幂等：重复执行只会重置该账号的密码、角色、状态
-- ============================================================
SET NAMES utf8mb4;

INSERT INTO user1 (name, pwd, role, status, real_name, phone, remark)
VALUES ('testuser',
        'e10adc3949ba59abbe56e057f20f883e', -- 123456 的 MD5
        'user',
        1,
        '测试旅客',
        '13800000000',
        '仅测试用，可随时删除')
ON DUPLICATE KEY UPDATE
    pwd    = VALUES(pwd),
    role   = 'user',
    status = 1,
    remark = '仅测试用，可随时删除';

-- 验证：SELECT name, role, status FROM user1 WHERE name = 'testuser';
