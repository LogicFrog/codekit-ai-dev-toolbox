-- 创建数据库
CREATE DATABASE IF NOT EXISTS codekit DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE codekit;

-- 1. 代码分类表
CREATE TABLE IF NOT EXISTS `code_category` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `category_name` varchar(100) NOT NULL COMMENT '分类名称',
    `sort_order` int DEFAULT 0 COMMENT '排序值',
    `create_time` datetime(6) DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime(6) DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_name` (`category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代码分类表';

-- 2. 代码片段表
CREATE TABLE IF NOT EXISTS `code_snippet` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_path` varchar(512) NOT NULL COMMENT '文件绝对路径',
    `file_name` varchar(255) NOT NULL COMMENT '文件名',
    `code_content` longtext COLLATE utf8mb4_general_ci COMMENT '代码内容',
    `language_type` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '语言类型',
    `file_md5` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文件内容MD5',
    `package_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '包名',
    `class_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '类名',
    `category_id` bigint DEFAULT NULL COMMENT '分类ID',
    `create_time` datetime(6) DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime(6) DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_path` (`file_path`),
    KEY `idx_category_id` (`category_id`),
    FULLTEXT KEY `ft_code_content` (`code_content`),
    FULLTEXT KEY `ft_file_name` (`file_name`),
    FULLTEXT KEY `ft_class_name` (`class_name`),
    FULLTEXT KEY `ft_combined` (`file_name`, `class_name`, `code_content`),
    CONSTRAINT `fk_code_snippet_category_id`
      FOREIGN KEY (`category_id`) REFERENCES `code_category` (`id`)
          ON DELETE SET NULL
          ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代码片段表';

-- 3. 代码标签表
CREATE TABLE IF NOT EXISTS `code_snippet_tags` (
   `snippet_id` bigint NOT NULL COMMENT '关联代码片段ID',
   `tag_name` varchar(100) NOT NULL COMMENT '标签名称',
   PRIMARY KEY (`snippet_id`, `tag_name`),
   KEY `idx_tag_name` (`tag_name`),
   CONSTRAINT `fk_snippet_tags_snippet_id`
       FOREIGN KEY (`snippet_id`) REFERENCES `code_snippet` (`id`)
           ON DELETE CASCADE
           ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代码标签关联表';

-- 4. 代码依赖表
CREATE TABLE IF NOT EXISTS `code_dependency` (
     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
     `code_snippet_id` bigint NOT NULL COMMENT '所属代码片段ID',
     `depend_name` varchar(255) NOT NULL COMMENT '依赖名称',
     `depend_type` varchar(50) DEFAULT NULL COMMENT '依赖类型',
     PRIMARY KEY (`id`),
     KEY `idx_code_snippet_id` (`code_snippet_id`),
     CONSTRAINT `fk_code_dependency_snippet_id`
         FOREIGN KEY (`code_snippet_id`) REFERENCES `code_snippet` (`id`)
             ON DELETE CASCADE
             ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代码依赖表';

-- 5. 版本信息表
CREATE TABLE IF NOT EXISTS `version_info` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `snippet_id` bigint NOT NULL COMMENT '关联代码片段ID',
    `version_name` varchar(50) DEFAULT NULL COMMENT '版本标识',
    `description` varchar(500) DEFAULT NULL COMMENT '版本描述',
    `code_content` text COMMENT '代码快照',
    `create_time` datetime(6) DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_snippet_id` (`snippet_id`),
    CONSTRAINT `fk_version_info_snippet_id`
      FOREIGN KEY (`snippet_id`) REFERENCES `code_snippet` (`id`)
          ON DELETE CASCADE
          ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='版本信息表';

-- 6. 检索历史表
CREATE TABLE IF NOT EXISTS `search_history` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `keyword` varchar(200) NOT NULL COMMENT '检索关键词',
    `search_type` int DEFAULT 0 COMMENT '检索类型(0:关键词, 1:语义)',
    `search_time` datetime(6) DEFAULT NULL COMMENT '检索时间',
    PRIMARY KEY (`id`),
    KEY `idx_search_time` (`search_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='检索历史表';

-- 7. 代码向量表（语义检索）
CREATE TABLE IF NOT EXISTS `code_embedding` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `snippet_id` bigint NOT NULL COMMENT '代码片段ID',
    `embedding_json` longtext COLLATE utf8mb4_general_ci NOT NULL COMMENT '向量(JSON数组)',
    `embedding_dim` int NOT NULL COMMENT '向量维度',
    `model_name` varchar(100) DEFAULT NULL COMMENT '向量模型名',
    `create_time` datetime(6) DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime(6) DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_snippet_id` (`snippet_id`),
    KEY `idx_update_time` (`update_time`),
    CONSTRAINT `fk_embedding_snippet_id`
        FOREIGN KEY (`snippet_id`) REFERENCES `code_snippet` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代码向量表';