package org.itfjnu.codekit.common.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.itfjnu.codekit.common.dto.ApiResponse;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.itfjnu.codekit.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理所有异常，返回标准化的错误响应
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ========== 参数校验异常 ==========

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        String msg = ErrorCode.PARAM_INVALID.getMessage() + ": " + detail;
        log.warn("参数校验失败: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.PARAM_INVALID, msg));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        String msg = ErrorCode.PARAM_INVALID.getMessage() + ": " + detail;
        log.warn("参数绑定失败: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.PARAM_INVALID, msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(this::formatConstraintViolation)
                .collect(Collectors.joining("; "));
        String msg = ErrorCode.PARAM_INVALID.getMessage() + ": " + detail;
        log.warn("约束校验失败: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.PARAM_INVALID, msg));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        String msg = String.format("缺少必要参数: %s", ex.getParameterName());
        log.warn("缺少参数: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.PARAM_MISSING, msg));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = String.format("参数类型错误: %s 应为 %s 类型", 
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "未知");
        log.warn("参数类型错误: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.PARAM_TYPE_ERROR, msg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = ErrorCode.PARAM_INVALID.getMessage() + ": " + ex.getMessage();
        log.debug("非法参数: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.PARAM_INVALID, msg));
    }

    // ========== HTTP 请求异常 ==========

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String msg = "请求体格式错误或缺失";
        log.warn("请求体不可读: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.PARAM_INVALID, msg));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String msg = String.format("请求方法 %s 不支持，支持的方法: %s", 
                ex.getMethod(), ex.getSupportedHttpMethods());
        log.warn("请求方法不支持: {}", msg);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail(ErrorCode.METHOD_NOT_ALLOWED, msg));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        String msg = String.format("不支持的媒体类型: %s，支持的类型: %s", 
                ex.getContentType(), ex.getSupportedMediaTypes());
        log.warn("媒体类型不支持: {}", msg);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.fail(ErrorCode.MEDIA_TYPE_NOT_SUPPORTED, msg));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        String msg = String.format("上传文件大小超出限制，最大允许: %s", ex.getMaxUploadSize());
        log.warn("上传文件过大: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.PARAM_LENGTH_EXCEEDED, msg));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException ex) {
        String msg = String.format("请求路径不存在: %s", ex.getRequestURL());
        log.warn("请求路径不存在: {}", msg);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ErrorCode.NOT_FOUND, msg));
    }

    // ========== 权限异常 ==========

    // @ExceptionHandler(AccessDeniedException.class)
    // public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
    //     String msg = "无权限访问该资源";
    //     log.warn("访问被拒绝: {}", ex.getMessage());
    //     return ResponseEntity.status(HttpStatus.FORBIDDEN)
    //             .body(ApiResponse.fail(ErrorCode.FORBIDDEN, msg));
    // }

    // ========== 数据库异常 ==========

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String msg = "数据完整性约束违反";
        log.error("数据完整性异常: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.DATABASE_TRANSACTION_FAILED, msg));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException ex) {
        String msg = "数据库访问异常";
        log.error("数据库访问异常: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.DATABASE_ERROR, msg));
    }

    // ========== 业务异常 ==========

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        String msg = ex.getMessage();
        log.warn("业务异常: {}", msg);
        return createResponse(ex.getErrorCode(), msg);
    }

    // ========== 服务异常 ==========

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceException(ServiceException ex) {
        String msg = ex.getMessage();
        log.error("服务异常: {}", msg, ex);
        return createResponse(ex.getErrorCode(), msg);
    }

    // ========== 未知异常 ==========

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(Exception ex) {
        log.error("系统未处理异常", ex);
        return createResponse(ErrorCode.SERVER_ERROR, "系统内部繁忙，请稍后再试");
    }

    // ========== 辅助方法 ==========

    /**
     * 格式化字段错误
     */
    private String formatFieldError(FieldError error) {
        String field = error.getField();
        String message = error.getDefaultMessage();
        return String.format("%s %s", field, message != null ? message : "校验失败");
    }

    /**
     * 格式化约束违反
     */
    private String formatConstraintViolation(ConstraintViolation<?> violation) {
        String property = violation.getPropertyPath().toString();
        String message = violation.getMessage();
        return String.format("%s %s", property, message != null ? message : "校验失败");
    }

    /**
     * 创建响应
     */
    private ResponseEntity<ApiResponse<Void>> createResponse(ErrorCode errorCode, String message) {
        HttpStatus status = switch (errorCode) {
            case UNAUTHORIZED, AI_INVALID_API_KEY, AI_API_KEY_EXPIRED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, FILE_PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case NOT_FOUND, CODE_NOT_FOUND, VERSION_NOT_FOUND, FILE_NOT_FOUND, 
                 DIRECTORY_NOT_FOUND, GIT_REPOSITORY_NOT_FOUND, GIT_BRANCH_NOT_FOUND, 
                 GIT_COMMIT_NOT_FOUND, CODE_DEPENDENCY_NOT_FOUND, CODE_TAG_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case METHOD_NOT_ALLOWED -> HttpStatus.METHOD_NOT_ALLOWED;
            case MEDIA_TYPE_NOT_SUPPORTED -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case REQUEST_TIMEOUT, SEARCH_TIMEOUT, AI_REQUEST_TIMEOUT -> HttpStatus.REQUEST_TIMEOUT;
            case TOO_MANY_REQUESTS, AI_RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            case FEATURE_NOT_IMPLEMENTED -> HttpStatus.NOT_IMPLEMENTED;
            case AI_REQUEST_FAILED, AI_RESPONSE_READ_FAILED, AI_RESPONSE_PARSE_FAILED,
                 AI_EMPTY_RESPONSE, AI_FAILED -> HttpStatus.BAD_GATEWAY;
            case AI_MODEL_NOT_AVAILABLE, AI_SERVICE_UNAVAILABLE, AI_STREAM_INTERRUPTED ->
                    HttpStatus.SERVICE_UNAVAILABLE;
            case CODE_SAVE_FAILED, CODE_DELETE_FAILED, CODE_PARSE_FAILED, CODE_SCAN_FAILED,
                 CODE_FILE_READ_FAILED, CODE_FILE_WRITE_FAILED,
                 SEARCH_INDEX_BUILD_FAILED, SEARCH_INDEX_UPDATE_FAILED, SEARCH_FAILED,
                 VERSION_CREATE_FAILED, VERSION_DELETE_FAILED, VERSION_COMPARE_FAILED,
                 VERSION_RESTORE_FAILED, VERSION_LIST_FAILED, GIT_OPERATION_FAILED,
                 FILE_READ_ERROR, FILE_WRITE_ERROR, FILE_DELETE_ERROR, FILE_COPY_ERROR,
                 FILE_MOVE_ERROR, DIRECTORY_CREATE_FAILED, DIRECTORY_DELETE_FAILED,
                 WATCH_SERVICE_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            case SERVER_ERROR, SYSTEM_ERROR, DATABASE_ERROR, DATABASE_CONNECTION_FAILED, 
                 DATABASE_QUERY_FAILED, DATABASE_UPDATE_FAILED, DATABASE_TRANSACTION_FAILED, 
                 CACHE_ERROR, NETWORK_ERROR, CONFIG_ERROR, UNKNOWN_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };

        String finalMsg = message != null ? message : errorCode.getMessage();
        return ResponseEntity.status(status).body(ApiResponse.fail(errorCode, finalMsg));
    }
}
