package org.itfjnu.codekit.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 通用返回结构
 * 
 * @param <T> 数据类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;

    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 判断是否成功
     */
    public Boolean isSuccess() {
        return this.code == ErrorCode.SUCCESS.getCode();
    }

    /**
     * 判断是否为客户端错误
     */
    public Boolean isClientError() {
        ErrorCode errorCode = ErrorCode.fromCode(this.code);
        return errorCode != null && errorCode.isClientError();
    }

    /**
     * 判断是否为业务错误
     */
    public Boolean isBusinessError() {
        ErrorCode errorCode = ErrorCode.fromCode(this.code);
        return errorCode != null && errorCode.isBusinessError();
    }

    /**
     * 判断是否为系统错误
     */
    public Boolean isSystemError() {
        ErrorCode errorCode = ErrorCode.fromCode(this.code);
        return errorCode != null && errorCode.isSystemError();
    }

    // ========== 成功响应 ==========

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), message, data);
    }

    // ========== 失败响应 ==========

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message != null ? message : errorCode.getMessage(), null);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(ErrorCode.BAD_REQUEST.getCode(), message, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    // ========== Getter/Setter ==========

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
