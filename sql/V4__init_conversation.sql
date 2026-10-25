-- =====================================================================
-- let-out-house  V4：会话 + 消息
--
-- 关键设计说明：
--   1. 【双层隔离】tenant_id 只隔离租户；同一租户内不同用户的会话也必须隔离，
--      靠 conversation.user_id 实现。MyBatis-Plus 的租户拦截器只管 tenant_id，
--      user_id 条件必须由业务层显式拼，漏了就是"同事能看到你的聊天记录"。
--   2. 【user_id 不叫 created_by】会话归属人就是创建人，不设两个字段，
--      避免"到底该按哪个查"的混乱。
--   3. 【message 无 deleted】消息不单独删除；删会话只逻辑删 conversation，
--      消息保留（将来做归档/审计有用）。
--   4. 【content 用 MEDIUMTEXT】LLM 回答 + RAG 引用片段可能很长，
--      TEXT(64KB) 在长文档问答场景下会截断。
-- 执行账号：loh_migrate
-- 幂等：可重复执行
-- =====================================================================

USE `let_out_house`;

-- ---------------------------------------------------------------------
-- 1. 会话表
--    last_message_at 冗余存储，用于列表按"最近活跃"排序；
--    否则每次都要 join message 取 max(created_at)，列表页会慢。
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `conversation`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话主键',
    `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '会话归属用户，同租户内按用户隔离',
    `agent_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联的Agent',
    `title`           VARCHAR(200)    NOT NULL DEFAULT '新对话' COMMENT '会话标题，首条提问自动截取',
    `message_count`   INT             NOT NULL DEFAULT 0 COMMENT '消息条数，冗余计数',
    `last_message_at` DATETIME(3)              DEFAULT NULL COMMENT '最后一条消息时间，列表排序用',
    `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '1正常 0归档',
    `created_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted`         BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    -- 会话列表的主查询路径：当前租户 + 当前用户 + 按活跃时间倒序
    KEY `idx_tenant_user_active` (`tenant_id`, `user_id`, `last_message_at`),
    KEY `idx_tenant_agent` (`tenant_id`, `agent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='会话表';

-- ---------------------------------------------------------------------
-- 2. 消息表
--    role: user=用户提问 / assistant=模型回答 / system=系统提示（一般不入库，留扩展）
--    trace_id 记录产生该条消息的链路追踪号，排查"这条回答为什么是这样"时用
--    token_input/output 供 billing-log-service 消费，不重复计算
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `message`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息主键',
    `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `conversation_id` BIGINT UNSIGNED NOT NULL COMMENT '所属会话',
    `role`            VARCHAR(16)     NOT NULL COMMENT 'user / assistant / system',
    `content`         MEDIUMTEXT      NOT NULL COMMENT '消息内容',
    `token_input`     INT                      DEFAULT NULL COMMENT '本次请求消耗的输入token',
    `token_output`    INT                      DEFAULT NULL COMMENT '本次回答消耗的输出token',
    `msg_status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '1成功 0失败（失败也要留痕）',
    `trace_id`        VARCHAR(64)              DEFAULT NULL COMMENT '产生该消息的链路追踪号',
    `created_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    -- 历史消息分页：按会话取、按 id 递增（自增 id 天然有序，比 created_at 更稳）
    KEY `idx_conv_id` (`tenant_id`, `conversation_id`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='会话消息表';

-- =====================================================================
-- 3. 自检
-- =====================================================================
SELECT 'conversation' AS tbl, COUNT(*) AS cnt FROM `conversation`
UNION ALL SELECT 'message', COUNT(*) FROM `message`;
