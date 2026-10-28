-- =====================================================================
-- let-out-house  V5：文档 + 分片（RAG 检索链路的 MySQL 侧）
--
-- 关键设计说明：
--   1. 【向量只存 Milvus】document_chunk 只存元数据 + milvus_pk，
--      检索时先查 Milvus 拿 pk，再回 MySQL 取正文；MySQL 不存向量列。
--   2. 【document_chunk 不加 deleted】分片随文档重新解析而整体重建
--      （先物理删旧分片再写新分片），逻辑删除会让表无限膨胀；
--      逻辑删除只发生在 document 层。
--   3. 【parse_status 独立于 status】解析状态（待解析/解析中/完成/失败）
--      是流水线状态；status 是业务开关（停用的文档不参与检索）。
--   4. 【milvus_pk 用 BIGINT】Milvus standalone 的自增主键就是 int64，
--      直接对应，检索回查 WHERE milvus_pk IN (...) 走索引。
-- 执行账号：loh_migrate
-- 幂等：可重复执行
-- =====================================================================

USE `let_out_house`;

-- ---------------------------------------------------------------------
-- 1. 文档表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `document`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文档主键',
    `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `kb_id`        BIGINT UNSIGNED NOT NULL COMMENT '所属知识库ID',
    `doc_name`     VARCHAR(255)    NOT NULL COMMENT '文档名称（含扩展名）',
    `file_type`    VARCHAR(16)     NOT NULL COMMENT '文件类型：pdf/docx/md/txt',
    `file_size`    BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文件字节数',
    `storage_key`  VARCHAR(512)    NOT NULL COMMENT '对象存储/本地路径标识',
    `chunk_count`  INT             NOT NULL DEFAULT 0 COMMENT '分片数量，解析完成后回写',
    `parse_status` TINYINT         NOT NULL DEFAULT 0 COMMENT '解析状态：0待解析 1解析中 2完成 3失败',
    `fail_reason`  VARCHAR(512)             DEFAULT NULL COMMENT '解析失败原因',
    `status`       TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0停用（停用不参与检索）',
    `created_by`   BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `created_at`   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_by`   BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `updated_at`   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted`      BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_kb_doc` (`tenant_id`, `kb_id`, `doc_name`, `deleted`),
    KEY `idx_kb_parse` (`kb_id`, `parse_status`),
    KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='知识库文档';

-- ---------------------------------------------------------------------
-- 2. 分片表（元数据，向量在 Milvus）
--    检索路径：query -> embedding -> Milvus topK -> milvus_pk 集合
--             -> SELECT * FROM document_chunk WHERE milvus_pk IN (...) AND status=1
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `document_chunk`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分片主键',
    `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `kb_id`        BIGINT UNSIGNED NOT NULL COMMENT '所属知识库ID（冗余，免join）',
    `document_id`  BIGINT UNSIGNED NOT NULL COMMENT '所属文档ID',
    `chunk_index`  INT             NOT NULL COMMENT '分片在文档内的序号，从0开始',
    `content`      TEXT            NOT NULL COMMENT '分片正文',
    `char_count`   INT             NOT NULL DEFAULT 0 COMMENT '正文字符数',
    `milvus_pk`    BIGINT UNSIGNED NOT NULL COMMENT 'Milvus 自增主键，检索回查用',
    `status`       TINYINT         NOT NULL DEFAULT 1 COMMENT '1有效 0失效（文档停用/重解析中置0）',
    `created_at`   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_doc_chunk` (`tenant_id`, `document_id`, `chunk_index`),
    KEY `idx_milvus_pk` (`milvus_pk`),
    KEY `idx_kb_status` (`kb_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='文档分片（向量存Milvus）';
