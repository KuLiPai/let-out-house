-- =====================================================================
-- let-out-house  V1 初始化脚本：建库 / 建账号 / 租户 + RBAC 六表
-- 适用：MySQL 8.0.x（本机 8.0.45 已验证）
-- 执行账号：root（或具备 CREATE DATABASE / CREATE USER 权限的账号）
-- 幂等：可重复执行（IF NOT EXISTS + INSERT IGNORE）
-- 注意：正式交付前必须修改下方三个账号的初始密码
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 建库：字符集与排序规则在建库时一次性定死，后期修改代价极高
--    utf8mb4 才能存 emoji 与生僻字；RAG 场景会灌入大量中文正文
-- ---------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `let_out_house`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 2. 建账号：禁止业务进程使用 root
--    loh_app     -> 业务读写（DML），日常服务连接用这个
--    loh_migrate -> 结构变更（DDL），只在执行脚本/迁移时使用
--    loh_ro      -> 只读，运维排查与报表用
-- ---------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'loh_app'@'%'         IDENTIFIED BY 'LohApp_2026_Dev';
CREATE USER IF NOT EXISTS 'loh_app'@'localhost' IDENTIFIED BY 'LohApp_2026_Dev';
CREATE USER IF NOT EXISTS 'loh_migrate'@'%'     IDENTIFIED BY 'LohMigrate_2026_Dev';
CREATE USER IF NOT EXISTS 'loh_ro'@'%'          IDENTIFIED BY 'LohRo_2026_Dev';

GRANT SELECT, INSERT, UPDATE, DELETE ON `let_out_house`.* TO 'loh_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `let_out_house`.* TO 'loh_app'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES
    ON `let_out_house`.* TO 'loh_migrate'@'%';
GRANT SELECT ON `let_out_house`.* TO 'loh_ro'@'%';
FLUSH PRIVILEGES;

USE `let_out_house`;

-- ---------------------------------------------------------------------
-- 3. 租户表
--    tenant_code 用于派生 MinIO 目录（bucket/{tenant_code}/...）
--    与 Milvus Collection 名（kb_{tenant_code}_{kb_id}），必须全局唯一且不含特殊字符
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tenant`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '租户主键',
    `tenant_code`  VARCHAR(64)     NOT NULL COMMENT '租户编码，全局唯一，仅允许小写字母/数字/下划线',
    `tenant_name`  VARCHAR(128)    NOT NULL COMMENT '租户名称',
    `contact_name` VARCHAR(64)              DEFAULT NULL COMMENT '联系人',
    `contact_phone` VARCHAR(32)             DEFAULT NULL COMMENT '联系电话',
    `status`       TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    `expire_at`    DATETIME(3)              DEFAULT NULL COMMENT '到期时间，NULL 表示不限',
    `remark`       VARCHAR(512)             DEFAULT NULL COMMENT '备注',
    `created_by`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人，0 表示系统',
    `created_at`   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_by`   BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `updated_at`   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted`      BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除，删除时写入自身id（规避逻辑删除与唯一键冲突）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_code`, `deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='租户表';

-- ---------------------------------------------------------------------
-- 4. 用户表
--    唯一键必须带 tenant_id 前缀，否则 A 公司与 B 公司的同名账号会互相顶掉
--    deleted 参与唯一键：逻辑删除后同名账号仍可重建（删除时 deleted 写自身 id）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `username`      VARCHAR(64)     NOT NULL COMMENT '登录名，租户内唯一',
    `password_hash` VARCHAR(100)    NOT NULL COMMENT 'BCrypt 哈希，禁止存明文/MD5',
    `real_name`     VARCHAR(64)              DEFAULT NULL COMMENT '姓名',
    `email`         VARCHAR(128)             DEFAULT NULL COMMENT '邮箱',
    `phone`         VARCHAR(32)              DEFAULT NULL COMMENT '手机号',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    `last_login_at` DATETIME(3)              DEFAULT NULL COMMENT '最后登录时间',
    `created_by`    BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `created_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_by`    BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `updated_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted`       BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_username` (`tenant_id`, `username`, `deleted`),
    KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户表';

-- ---------------------------------------------------------------------
-- 5. 角色表
--    builtin=1 表示系统内置角色，禁止业务侧删除/改码
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色主键',
    `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `role_code`   VARCHAR(64)     NOT NULL COMMENT '角色编码，租户内唯一',
    `role_name`   VARCHAR(64)     NOT NULL COMMENT '角色名称',
    `description` VARCHAR(255)             DEFAULT NULL,
    `builtin`     TINYINT         NOT NULL DEFAULT 0 COMMENT '1内置角色，不可删除',
    `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    `created_by`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `created_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_by`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `updated_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted`     BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_role_code` (`tenant_id`, `role_code`, `deleted`),
    KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='角色表';

-- ---------------------------------------------------------------------
-- 6. 权限点表
--    tenant_id = 0 表示平台内置权限字典，所有租户共享、只读
--    租户自定义权限点才写入自己的 tenant_id
--    鉴权查询条件固定为：WHERE tenant_id IN (0, :currentTenantId)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_permission`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限主键',
    `tenant_id`     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=平台内置',
    `parent_id`     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父权限，0为根',
    `perm_code`     VARCHAR(128)    NOT NULL COMMENT '权限编码，如 kb:document:upload',
    `perm_name`     VARCHAR(64)     NOT NULL COMMENT '权限名称',
    `resource_type` VARCHAR(16)     NOT NULL DEFAULT 'API' COMMENT 'API/MENU/ACTION',
    `api_path`      VARCHAR(255)             DEFAULT NULL COMMENT '对应接口路径，可含通配符',
    `http_method`   VARCHAR(16)              DEFAULT NULL COMMENT 'GET/POST/PUT/DELETE',
    `sort`          INT             NOT NULL DEFAULT 0 COMMENT '排序',
    `status`        TINYINT         NOT NULL DEFAULT 1,
    `created_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted`       BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_perm_code` (`tenant_id`, `perm_code`, `deleted`),
    KEY `idx_parent` (`parent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='权限点表';

-- ---------------------------------------------------------------------
-- 7. 用户-角色关联
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user_role`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT '冗余租户ID，所有查询必带，避免跨租户关联',
    `user_id`    BIGINT UNSIGNED NOT NULL,
    `role_id`    BIGINT UNSIGNED NOT NULL,
    `created_by` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user_role` (`tenant_id`, `user_id`, `role_id`),
    KEY `idx_role` (`tenant_id`, `role_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户角色关联表';

-- ---------------------------------------------------------------------
-- 8. 角色-权限关联
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role_permission`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id`  BIGINT UNSIGNED NOT NULL,
    `role_id`    BIGINT UNSIGNED NOT NULL,
    `perm_id`    BIGINT UNSIGNED NOT NULL,
    `created_by` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_role_perm` (`tenant_id`, `role_id`, `perm_id`),
    KEY `idx_perm` (`tenant_id`, `perm_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='角色权限关联表';

-- =====================================================================
-- 9. 初始化数据
-- =====================================================================

-- 平台租户（tenant_id=1，内部管理用）
INSERT IGNORE INTO `tenant` (`id`, `tenant_code`, `tenant_name`, `status`, `remark`)
VALUES (1, 'platform', '平台租户', 1, '系统内置租户，承载平台管理员');

-- 内置角色
INSERT IGNORE INTO `sys_role` (`id`, `tenant_id`, `role_code`, `role_name`, `description`, `builtin`)
VALUES (1, 1, 'admin', '租户管理员', '租户内最高权限，内置不可删除', 1),
       (2, 1, 'member', '普通成员', '仅可对话与查看自己的会话', 1);

-- 内置权限点字典（tenant_id=0）
-- 路径风格对齐契约：/api/{资源}，不带版本号前缀
INSERT IGNORE INTO `sys_permission` (`id`, `tenant_id`, `parent_id`, `perm_code`, `perm_name`, `resource_type`, `api_path`, `http_method`, `sort`)
VALUES (1, 0, 0, 'tenant:manage', '租户管理', 'API', '/api/tenant/**', '*', 10),
       (2, 0, 0, 'user:read', '查看用户', 'API', '/api/user/**', 'GET', 20),
       (3, 0, 0, 'user:write', '管理用户', 'API', '/api/user/**', 'POST', 21),
       (4, 0, 0, 'role:manage', '角色权限管理', 'API', '/api/role/**', '*', 30),
       (5, 0, 0, 'agent:read', '查看Agent配置', 'API', '/api/agent/**', 'GET', 40),
       (6, 0, 0, 'agent:write', '管理Agent配置', 'API', '/api/agent/**', 'POST', 41),
       (7, 0, 0, 'kb:read', '查看知识库', 'API', '/api/knowledge-base/**', 'GET', 50),
       (8, 0, 0, 'kb:write', '管理知识库', 'API', '/api/knowledge-base/**', 'POST', 51),
       (9, 0, 0, 'kb:document:upload', '上传文档', 'API', '/api/knowledge-base/*/upload', 'POST', 52),
       (10, 0, 0, 'kb:query', '知识库检索', 'API', '/api/rag/retrieve', 'POST', 53),
       (11, 0, 0, 'conversation:read', '查看会话', 'API', '/api/conversation/**', 'GET', 60),
       (12, 0, 0, 'conversation:chat', '发起对话', 'API', '/api/conversation/*/stream', 'POST', 61),
       (13, 0, 0, 'conversation:delete', '删除会话', 'API', '/api/conversation/**', 'DELETE', 62),
       (14, 0, 0, 'task:read', '查看任务', 'API', '/api/task/**', 'GET', 70),
       (15, 0, 0, 'task:write', '创建/取消任务', 'API', '/api/task/**', 'POST', 71),
       (16, 0, 0, 'billing:read', '查看计费', 'API', '/api/billing/**', 'GET', 80),
       (17, 0, 0, 'audit:read', '查看审计日志', 'API', '/api/audit/**', 'GET', 90);

-- 默认管理员：admin / Admin@12345（BCrypt，cost=10）
-- 首次登录后必须强制修改；正式交付前请在平台侧重置
INSERT IGNORE INTO `sys_user` (`id`, `tenant_id`, `username`, `password_hash`, `real_name`, `status`)
VALUES (1, 1, 'admin', '$2b$10$h.G.5Hx5rvFmu7dw1OLHGOAl3.M45ertIqo054D6sCljFZmeU6H1K', '平台管理员', 1);

-- 管理员绑定 admin 角色
INSERT IGNORE INTO `sys_user_role` (`id`, `tenant_id`, `user_id`, `role_id`)
VALUES (1, 1, 1, 1);

-- admin 角色授予全部内置权限
INSERT IGNORE INTO `sys_role_permission` (`tenant_id`, `role_id`, `perm_id`)
SELECT 1, 1, p.`id`
FROM `sys_permission` p
WHERE p.`tenant_id` = 0
  AND p.`deleted` = 0;

-- =====================================================================
-- 10. 自检：执行完应看到 6 张表 + 租户 1 条 + 权限 17 条
-- =====================================================================
SELECT 'tenant' AS tbl, COUNT(*) AS cnt FROM `tenant`
UNION ALL SELECT 'sys_user', COUNT(*) FROM `sys_user`
UNION ALL SELECT 'sys_role', COUNT(*) FROM `sys_role`
UNION ALL SELECT 'sys_permission', COUNT(*) FROM `sys_permission`
UNION ALL SELECT 'sys_user_role', COUNT(*) FROM `sys_user_role`
UNION ALL SELECT 'sys_role_permission', COUNT(*) FROM `sys_role_permission`;
