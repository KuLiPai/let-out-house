-- =====================================================================
-- let-out-house  V2 迁移：对齐接口契约的权限点路径
--
-- 背景：V1 里权限点写的是 /api/v1/**，与最终契约 /api/** 不一致。
--       网关做 URL 级权限匹配时会全部匹配失败，表现为"所有人 403"。
--
-- 影响范围：仅 sys_permission（字典表）与 sys_role_permission（关联）
-- 不涉及业务数据，可安全重复执行
-- 执行账号：loh_migrate
-- =====================================================================

USE `let_out_house`;

-- 1. 先清关联，避免外键式脏数据（本表无物理外键，靠顺序保证一致性）
DELETE FROM `sys_role_permission` WHERE `tenant_id` = 1 AND `role_id` = 1;

-- 2. 清掉旧权限点（保留租户自定义权限点：tenant_id <> 0 的不动）
DELETE FROM `sys_permission` WHERE `tenant_id` = 0;

-- 3. 重新插入，显式指定 id，保证与原关联关系可重建
INSERT INTO `sys_permission` (`id`, `tenant_id`, `parent_id`, `perm_code`, `perm_name`, `resource_type`, `api_path`, `http_method`, `sort`)
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

-- 4. 重建 admin 角色的授权关系
INSERT INTO `sys_role_permission` (`tenant_id`, `role_id`, `perm_id`, `created_by`)
SELECT 1, 1, p.`id`, 0
FROM `sys_permission` p
WHERE p.`tenant_id` = 0
  AND p.`deleted` = 0;

-- 5. 自检：应返回 17 条，且没有一条包含 /api/v1
SELECT COUNT(*) AS perm_count,
       SUM(CASE WHEN `api_path` LIKE '/api/v1/%' THEN 1 ELSE 0 END) AS stale_v1_count
FROM `sys_permission`
WHERE `tenant_id` = 0 AND `deleted` = 0;
