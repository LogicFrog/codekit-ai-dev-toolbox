package org.itfjnu.codekit.code.filesystem;

/**
 * 文件扫描常量：定义支持的代码语言、排除的文件/目录
 */
public class FileScanConstant {
    // 支持的代码文件后缀（可扩展）
    public static final String[] SUPPORTED_SUFFIX = {
        ".java", ".py", ".js", ".vue", ".ts", ".jsx", ".tsx"
    };

    // 排除的目录名称（无需扫描的目录）
    public static final String[] EXCLUDE_DIR = {
        // 版本控制
        ".git", ".svn", ".hg",
        
        // IDE 配置
        ".idea", ".vscode", ".vs",
        
        // 构建输出
        "target", "build", "dist", "out", ".gradle",
        
        // 依赖目录
        "node_modules", "vendor", "venv", "__pycache__",
        
        // 系统文件
        ".DS_Store", "Thumbs.db",
        
        // 日志和临时文件
        "logs", "tmp", "temp",
        
        // 本地仓库（避免扫描到系统其他项目）
        "repository"
    };

    // 排除的文件名称
    public static final String[] EXCLUDE_FILE = {
        // 配置文件
        ".log", ".xml", ".yml", ".yaml", ".properties",
        
        // 文档文件
        ".md", ".txt", ".rst",
        
        // 构建配置
        "pom.xml", "build.gradle", "package.json", "requirements.txt",
        
        // 其他
        ".gitignore", ".dockerignore", "LICENSE", "README"
    };
}
