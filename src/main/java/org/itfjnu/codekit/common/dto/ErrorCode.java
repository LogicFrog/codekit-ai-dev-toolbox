package org.itfjnu.codekit.common.dto;

/**
 * 通用错误码枚举
 * 
 * 错误码设计规范：
 * - 0: 成功
 * - 1xxx: 通用错误
 * - 2xxx: 参数校验错误
 * - 3xxx: 代码管理模块错误
 * - 4xxx: 检索模块错误
 * - 5xxx: AI 模块错误
 * - 6xxx: 版本管理模块错误
 * - 7xxx: 文件系统错误
 * - 9xxx: 系统错误
 */
public enum ErrorCode {
    // ========== 成功 ==========
    SUCCESS(0, "操作成功"),

    // ========== 通用错误 (1xxx) ==========
    BAD_REQUEST(1000, "请求参数错误"),
    UNAUTHORIZED(1001, "未授权，请先登录"),
    FORBIDDEN(1002, "无权限访问该资源"),
    NOT_FOUND(1003, "资源不存在"),
    METHOD_NOT_ALLOWED(1004, "请求方法不支持"),
    MEDIA_TYPE_NOT_SUPPORTED(1005, "不支持的媒体类型"),
    REQUEST_TIMEOUT(1006, "请求超时"),
    TOO_MANY_REQUESTS(1007, "请求过于频繁，请稍后再试"),
    SERVER_ERROR(1999, "服务器内部错误"),

    // ========== 参数校验错误 (2xxx) ==========
    PARAM_MISSING(2000, "缺少必要参数"),
    PARAM_INVALID(2001, "参数格式错误"),
    PARAM_TYPE_ERROR(2002, "参数类型错误"),
    PARAM_OUT_OF_RANGE(2003, "参数超出允许范围"),
    PARAM_FORMAT_ERROR(2004, "参数格式不正确"),
    PARAM_LENGTH_EXCEEDED(2005, "参数长度超出限制"),
    PARAM_DUPLICATE(2006, "参数重复"),
    PARAM_NOT_ALLOWED(2007, "不允许的参数值"),

    // ========== 代码管理模块错误 (3xxx) ==========
    CODE_NOT_FOUND(3000, "代码片段不存在"),
    CODE_ALREADY_EXISTS(3001, "代码片段已存在"),
    CODE_SAVE_FAILED(3002, "代码片段保存失败"),
    CODE_DELETE_FAILED(3003, "代码片段删除失败"),
    CODE_PARSE_FAILED(3004, "代码解析失败"),
    CODE_SCAN_FAILED(3005, "代码扫描失败"),
    CODE_SCAN_RUNNING(3006, "代码扫描任务正在运行中"),
    CODE_SCAN_NOT_FOUND(3007, "未找到扫描任务"),
    CODE_DEPENDENCY_NOT_FOUND(3008, "代码依赖不存在"),
    CODE_FILE_NOT_EXISTS(3009, "代码文件不存在"),
    CODE_FILE_READ_FAILED(3010, "代码文件读取失败"),
    CODE_FILE_WRITE_FAILED(3011, "代码文件写入失败"),
    CODE_TAG_NOT_FOUND(3012, "代码标签不存在"),
    CODE_LANGUAGE_NOT_SUPPORTED(3013, "不支持该编程语言"),

    // ========== 检索模块错误 (4xxx) ==========
    SEARCH_KEYWORD_EMPTY(4000, "搜索关键词不能为空"),
    SEARCH_RESULT_EMPTY(4001, "未找到匹配的搜索结果"),
    SEARCH_INDEX_NOT_READY(4002, "搜索索引未就绪"),
    SEARCH_INDEX_BUILD_FAILED(4003, "搜索索引构建失败"),
    SEARCH_INDEX_UPDATE_FAILED(4004, "搜索索引更新失败"),
    SEARCH_TYPE_NOT_SUPPORTED(4005, "不支持的搜索类型"),
    SEARCH_QUERY_TOO_LONG(4006, "搜索查询过长"),
    SEARCH_TIMEOUT(4007, "搜索超时"),
    SEARCH_FAILED(4999, "搜索失败"),

    // ========== AI 模块错误 (5xxx) ==========
    AI_REQUEST_FAILED(5000, "AI 请求失败"),
    AI_RESPONSE_PARSE_FAILED(5001, "AI 响应解析失败"),
    AI_MODEL_NOT_AVAILABLE(5002, "AI 模型不可用"),
    AI_QUOTA_EXCEEDED(5003, "AI 配额已用完"),
    AI_REQUEST_TIMEOUT(5004, "AI 请求超时"),
    AI_RATE_LIMIT_EXCEEDED(5005, "AI 请求频率超限"),
    AI_INVALID_API_KEY(5006, "AI API 密钥无效"),
    AI_API_KEY_EXPIRED(5007, "AI API 密钥已过期"),
    AI_CONTEXT_TOO_LONG(5008, "AI 上下文过长"),
    AI_SERVICE_UNAVAILABLE(5009, "AI 服务暂时不可用"),
    AI_STREAM_INTERRUPTED(5010, "AI 流式响应中断"),
    AI_EMPTY_RESPONSE(5011, "AI 未返回有效回答"),
    AI_RESPONSE_READ_FAILED(5012, "AI 响应读取失败"),
    FEATURE_NOT_IMPLEMENTED(5998, "功能尚未实现"),
    AI_FAILED(5999, "AI 处理失败"),

    // ========== 版本管理模块错误 (6xxx) ==========
    VERSION_NOT_FOUND(6000, "版本不存在"),
    VERSION_CREATE_FAILED(6001, "版本创建失败"),
    VERSION_DELETE_FAILED(6002, "版本删除失败"),
    VERSION_COMPARE_FAILED(6003, "版本对比失败"),
    VERSION_RESTORE_FAILED(6004, "版本恢复失败"),
    VERSION_CONFLICT(6005, "版本冲突"),
    VERSION_LOCKED(6006, "版本已锁定"),
    GIT_OPERATION_FAILED(6007, "Git 操作失败"),
    GIT_REPOSITORY_NOT_FOUND(6008, "Git 仓库不存在"),
    GIT_BRANCH_NOT_FOUND(6009, "Git 分支不存在"),
    GIT_COMMIT_NOT_FOUND(6010, "Git 提交记录不存在"),
    VERSION_LIST_FAILED(6011, "版本查询失败"),

    // ========== 文件系统错误 (7xxx) ==========
    FILE_NOT_FOUND(7000, "文件不存在"),
    FILE_READ_ERROR(7001, "文件读取失败"),
    FILE_WRITE_ERROR(7002, "文件写入失败"),
    FILE_DELETE_ERROR(7003, "文件删除失败"),
    FILE_COPY_ERROR(7004, "文件复制失败"),
    FILE_MOVE_ERROR(7005, "文件移动失败"),
    DIRECTORY_NOT_FOUND(7006, "目录不存在"),
    DIRECTORY_CREATE_FAILED(7007, "目录创建失败"),
    DIRECTORY_DELETE_FAILED(7008, "目录删除失败"),
    WATCH_SERVICE_ERROR(7009, "文件监控服务异常"),
    FILE_PATH_INVALID(7010, "文件路径无效"),
    FILE_PERMISSION_DENIED(7011, "文件权限不足"),

    // ========== 系统错误 (9xxx) ==========
    SYSTEM_ERROR(9000, "系统错误"),
    DATABASE_ERROR(9001, "数据库错误"),
    DATABASE_CONNECTION_FAILED(9002, "数据库连接失败"),
    DATABASE_QUERY_FAILED(9003, "数据库查询失败"),
    DATABASE_UPDATE_FAILED(9004, "数据库更新失败"),
    DATABASE_TRANSACTION_FAILED(9005, "数据库事务失败"),
    CACHE_ERROR(9006, "缓存错误"),
    NETWORK_ERROR(9007, "网络错误"),
    CONFIG_ERROR(9008, "配置错误"),
    UNKNOWN_ERROR(9999, "未知错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 根据错误码获取枚举
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return UNKNOWN_ERROR;
    }

    /**
     * 判断是否为成功
     */
    public boolean isSuccess() {
        return this.code == SUCCESS.code;
    }

    /**
     * 判断是否为客户端错误 (1xxx)
     */
    public boolean isClientError() {
        return this.code >= 1000 && this.code < 2000;
    }

    /**
     * 判断是否为业务错误 (3xxx-7xxx)
     */
    public boolean isBusinessError() {
        return (this.code >= 3000 && this.code < 8000);
    }

    /**
     * 判断是否为系统错误 (9xxx)
     */
    public boolean isSystemError() {
        return this.code >= 9000;
    }
}
