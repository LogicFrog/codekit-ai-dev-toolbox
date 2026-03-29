package org.itfjnu.codekit.common.exception;

import org.itfjnu.codekit.common.dto.ErrorCode;

/**
 * 服务异常
 * 用于处理服务层的异常情况，通常由系统内部错误引起
 */
public class ServiceException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 指定错误码（推荐用法）
     * 示例：throw new ServiceException(ErrorCode.DATABASE_ERROR);
     */
    public ServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 指定错误码和自定义消息
     * 示例：throw new ServiceException(ErrorCode.DATABASE_ERROR, "数据库连接超时");
     */
    public ServiceException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 指定错误码、自定义消息和原因
     */
    public ServiceException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 包装已有异常
     */
    public ServiceException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
