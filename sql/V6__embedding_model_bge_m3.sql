-- =====================================================================
-- let-out-house  V6：embedding 模型统一为硅基流动 BAAI/bge-m3
--
-- 背景：
--   原默认值 text-embedding-v3 是阿里 DashScope，项目没有该 key；
--   实测对比后选定 bge-m3（1024 维，硅基流动免费档，中文够用）。
--   该字段必须与实际调用的模型一致，否则排障时会怀疑人生。
-- 注意：
--   向量维度一旦由 1024 改动，Milvus collection 必须重建并重新灌数据。
-- 执行账号：loh_migrate
-- 幂等：可重复执行
-- =====================================================================

USE `let_out_house`;

ALTER TABLE `knowledge_base`
    MODIFY COLUMN `embedding_model` VARCHAR(64) NOT NULL DEFAULT 'BAAI/bge-m3'
        COMMENT '向量模型标识，须与 rag-service 实际调用一致';

UPDATE `knowledge_base`
SET `embedding_model` = 'BAAI/bge-m3'
WHERE `embedding_model` IN ('text-embedding-v3', 'text-embedding-v2', '');
