-- =====================================================================
-- let-out-house  V3：知识库 + Agent 配置 + Agent-知识库关联
--
-- 说明：
--   1. agent_kb_rel 是纯关联表，重新绑定用"先删后插"，因此不需要 deleted 字段
--   2. knowledge_base.collection_name 一旦写入不可修改（改名等于丢向量数据）
--   3. 不使用物理外键，引用完整性由应用层保证（与 RBAC 表保持一致）
--   4. 所有表带 tenant_id，租户隔离由 TenantLineInnerInterceptor 自动拼
-- 执行账号：loh_migrate（本地/服务器均可）
-- 幂等：可重复执行
-- =====================================================================

USE `let_out_house`;

-- ---------------------------------------------------------------------
-- 1. 知识库表
--    collection_name 是 Milvus 的 collection 名，规则：
--      只允许小写字母/数字/下划线，不能以数字开头，长度 <= 255
--      格式约定 kb_{tenantId}_{kbCode}，例：kb_1_faq
--    为什么不用自增 id 拼：插入时 id 还没生成，而 collection 名必须落库时确定
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `knowledge_base`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '知识库主键',
    `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `kb_code`         VARCHAR(64)     NOT NULL COMMENT '知识库编码，租户内唯一，仅小写字母/数字/下划线',
    `kb_name`         VARCHAR(128)    NOT NULL COMMENT '知识库名称',
    `description`     VARCHAR(512)             DEFAULT NULL COMMENT '描述',
    `embedding_model` VARCHAR(64)     NOT NULL DEFAULT 'text-embedding-v3' COMMENT '向量模型标识',
    `collection_name` VARCHAR(128)    NOT NULL COMMENT 'Milvus collection 名，落库后禁止修改',
    `doc_count`       INT             NOT NULL DEFAULT 0 COMMENT '文档数量，异步解析完成后回写',
    `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    `created_by`      BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `created_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_by`      BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `updated_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted`         BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_kb_code` (`tenant_id`, `kb_code`, `deleted`),
    UNIQUE KEY `uk_collection_name` (`collection_name`),
    KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='知识库表';

-- ---------------------------------------------------------------------
-- 2. Agent 配置表
--    temperature 用 DECIMAL(3,2)：0.00~9.99 区间足够，且避免浮点比较误差
--    top_k 是 RAG 检索返回条数，与 rag_enable 配合使用
--    实体类里不要声明 tenant_id（由拦截器填充）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `agent_config`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Agent主键',
    `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `agent_name`    VARCHAR(128)    NOT NULL COMMENT 'Agent名称，租户内唯一',
    `description`   VARCHAR(512)             DEFAULT NULL COMMENT '描述',
    `system_prompt` TEXT            NOT NULL COMMENT '系统提示词',
    `model_name`    VARCHAR(64)     NOT NULL COMMENT '模型标识，如 qwen-plus',
    `rag_enable`    TINYINT         NOT NULL DEFAULT 1 COMMENT '是否启用RAG检索 1开启 0关闭',
    `max_token`     INT             NOT NULL DEFAULT 2048 COMMENT '单次回答最大token',
    `temperature`   DECIMAL(3, 2)   NOT NULL DEFAULT 0.70 COMMENT '采样温度 0.00-2.00',
    `top_k`         INT             NOT NULL DEFAULT 3 COMMENT 'RAG检索返回条数',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    `created_by`    BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `created_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_by`    BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `updated_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted`       BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_agent_name` (`tenant_id`, `agent_name`, `deleted`),
    KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='Agent配置表';

-- ---------------------------------------------------------------------
-- 3. Agent - 知识库 关联表（多对多）
--    无 deleted：解绑就是物理删除，重新绑定先删后插
--    idx_kb 索引用于反查"这个知识库被哪些 Agent 使用"，
--    删除知识库前必须靠它检查引用，否则会留下悬空关联
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `agent_kb_rel`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT '冗余租户ID，所有查询必带',
    `agent_id`   BIGINT UNSIGNED NOT NULL COMMENT 'Agent ID',
    `kb_id`      BIGINT UNSIGNED NOT NULL COMMENT '知识库 ID',
    `created_by` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_agent_kb` (`tenant_id`, `agent_id`, `kb_id`),
    KEY `idx_kb` (`tenant_id`, `kb_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='Agent与知识库关联表';

-- =====================================================================
-- 4. 初始化数据：一个默认知识库 + 一个默认 Agent + 绑定关系
--    （方便前端联调，正式环境可删）
-- =====================================================================
INSERT IGNORE INTO `knowledge_base` (`id`, `tenant_id`, `kb_code`, `kb_name`, `description`, `collection_name`)
VALUES (1, 1, 'demo', '示例知识库', '开发联调用，可直接删除', 'kb_1_demo');

INSERT IGNORE INTO `agent_config` (`id`, `tenant_id`, `agent_name`, `description`, `system_prompt`, `model_name`)
VALUES (1, 1, '默认助手', '通用知识库问答助手',
        '你是企业内部知识库助手。请严格依据检索到的资料回答问题；资料中没有的内容，明确告知"未找到相关资料"，不要编造。',
        'qwen-plus');

INSERT IGNORE INTO `agent_kb_rel` (`tenant_id`, `agent_id`, `kb_id`)
VALUES (1, 1, 1);

-- =====================================================================
-- 5. 自检
-- =====================================================================
SELECT 'knowledge_base' AS tbl, COUNT(*) AS cnt FROM `knowledge_base` WHERE deleted = 0
UNION ALL SELECT 'agent_config', COUNT(*) FROM `agent_config` WHERE deleted = 0
UNION ALL SELECT 'agent_kb_rel', COUNT(*) FROM `agent_kb_rel`;
