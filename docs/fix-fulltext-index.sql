-- 检查 FULLTEXT 索引
SHOW INDEX FROM code_snippet WHERE Index_type = 'FULLTEXT';

-- 如果没有索引，创建 FULLTEXT 索引
-- 注意：FULLTEXT 索引只能添加到 CHAR、VARCHAR、TEXT 类型的列
-- 并且需要 InnoDB 引擎（MySQL 5.6+ 支持）

-- 删除旧的 FULLTEXT 索引（如果存在）
-- ALTER TABLE code_snippet DROP INDEX idx_fulltext_search;

-- 创建 FULLTEXT 索引
ALTER TABLE code_snippet ADD FULLTEXT INDEX idx_fulltext_search (file_name, class_name, code_content);

-- 查看索引是否创建成功
SHOW INDEX FROM code_snippet WHERE Index_type = 'FULLTEXT';

-- 测试全文搜索
-- SELECT * FROM code_snippet WHERE MATCH(file_name, class_name, code_content) AGAINST('service' IN NATURAL LANGUAGE MODE) LIMIT 10;
