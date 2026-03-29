-- 步骤 4：检查数据和测试搜索

USE codekit;

-- 1. 查看总数据量
SELECT COUNT(*) as total FROM code_snippet;

-- 2. 查看所有包含 "service" 的文件名（不区分大小写）
SELECT id, file_name, class_name, language_type
FROM code_snippet
WHERE LOWER(file_name) LIKE '%service%' OR LOWER(class_name) LIKE '%service%'
LIMIT 10;

-- 3. 测试全文搜索（小写）
SELECT id, file_name, class_name,
       SUBSTRING(code_content, 1, 100) as code_preview
FROM code_snippet
WHERE MATCH(file_name, class_name, code_content) AGAINST('service' IN NATURAL LANGUAGE MODE)
LIMIT 10;

-- 4. 测试全文搜索（大写）
SELECT id, file_name, class_name,
       SUBSTRING(code_content, 1, 100) as code_preview
FROM code_snippet
WHERE MATCH(file_name, class_name, code_content) AGAINST('Service' IN NATURAL LANGUAGE MODE)
LIMIT 10;

-- 5. 测试全文搜索（布尔模式）
SELECT id, file_name, class_name,
       SUBSTRING(code_content, 1, 100) as code_preview
FROM code_snippet
WHERE MATCH(file_name, class_name, code_content) AGAINST('*service*' IN BOOLEAN MODE)
LIMIT 10;

-- 6. 检查 FULLTEXT 索引的词频统计
SELECT * FROM information_schema.innodb_ft_index_table
WHERE word LIKE '%service%'
LIMIT 10;
