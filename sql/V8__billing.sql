-- =====================================================================
-- let-out-house  V8：计费（模型单价 + Token 用量明细）
--
-- 设计要点：
--   1. model_price 是平台级字典，没有 tenant_id，必须加进
--      CustomTenantLineHandler 的 IGNORE_TABLES，否则租户拦截器会往
--      SQL 里拼 tenant_id 条件导致 Unknown column。
--   2. token_usage_log 是 append-only 流水：不设 deleted 列，
--      纠错靠冲正记录，不靠逻辑删除。
--   3. 幂等键 uk_tenant_message(tenant_id, message_id)：
--      同一笔用量重复上报只落一条。
--   4. 金额 DECIMAL(12,6)，不用 double（token 少、单价小，浮点会攒误差）。
-- 执行账号：loh_migrate
-- 幂等：可重复执行
-- =====================================================================

USE `let_out_house`;

-- ---------- 1. 模型单价表（元 / 百万 token）----------
CREATE TABLE IF NOT EXISTS `model_price`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `model_name`   VARCHAR(64)     NOT NULL COMMENT '模型标识，如 glm-4-flash',
    `input_price`  DECIMAL(10, 4)  NOT NULL DEFAULT 0 COMMENT '输入单价，元/百万token',
    `output_price` DECIMAL(10, 4)  NOT NULL DEFAULT 0 COMMENT '输出单价，元/百万token',
    `currency`     VARCHAR(8)      NOT NULL DEFAULT 'CNY' COMMENT '币种',
    `status`       TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    `created_at`   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model` (`model_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='模型单价表（平台级，无租户）';

-- ---------- 2. Token 用量明细（append-only）----------
CREATE TABLE IF NOT EXISTS `token_usage_log`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `user_id`         BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '调用用户',
    `agent_id`        BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Agent ID',
    `conversation_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '会话ID',
    `message_id`      BIGINT UNSIGNED NOT NULL COMMENT 'message主键，幂等键',
    `model_name`      VARCHAR(64)     NOT NULL COMMENT '实际调用的模型',
    `input_tokens`    INT             NOT NULL DEFAULT 0,
    `output_tokens`   INT             NOT NULL DEFAULT 0,
    `input_cost`      DECIMAL(12, 6)  NOT NULL DEFAULT 0 COMMENT '输入费用，元',
    `output_cost`     DECIMAL(12, 6)  NOT NULL DEFAULT 0 COMMENT '输出费用，元',
    `total_cost`      DECIMAL(12, 6)  NOT NULL DEFAULT 0 COMMENT '合计费用，元',
    `request_id`      VARCHAR(64)              DEFAULT NULL COMMENT '链路请求ID',
    `created_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_message` (`tenant_id`, `message_id`),
    KEY `idx_tenant_created` (`tenant_id`, `created_at`),
    KEY `idx_tenant_model` (`tenant_id`, `model_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='Token用量明细（append-only）';

-- ---------- 3. 初始化单价（智谱免费档 + 常见商用模型参考价）----------
INSERT INTO `model_price` (`model_name`, `input_price`, `output_price`)
VALUES ('glm-4-flash', 0.0000, 0.0000),
       ('glm-4.7-flash', 0.0000, 0.0000),
       ('glm-4-air', 0.0700, 0.0700),
       ('glm-4-plus', 0.5000, 0.5000),
       ('qwen-plus', 0.8000, 2.0000)
ON DUPLICATE KEY UPDATE `input_price` = VALUES(`input_price`),
                        `output_price` = VALUES(`output_price`);
