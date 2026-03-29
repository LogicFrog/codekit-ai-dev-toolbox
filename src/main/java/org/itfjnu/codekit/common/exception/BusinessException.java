package org.itfjnu.codekit.common.exception;

import org.itfjnu.codekit.common.dto.ErrorCode;

/**
 * 业务异常
 * 用于处理业务逻辑中的异常情况，通常由客户端传参引起
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 指定错误码（推荐用法）
     * 示例：throw new BusinessException(ErrorCode.CODE_NOT_FOUND);
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 指定错误码和自定义消息
     * 示例：throw new BusinessException(ErrorCode.CODE_NOT_FOUND, "文件路径不存在：" + filePath);
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 指定错误码、自定义消息和原因
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 包装已有异常
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
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
