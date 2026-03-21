package com.openclaw.workflow.engine.smartdecompose.v2.client;

/**
 * OpenClaw 调用异常
 *
 * 当 OpenClaw Gateway 调用失败时抛出。
 */
public class OpenClawException extends RuntimeException {

    /** 错误类型 */
    private final ErrorCode errorCode;

    public enum ErrorCode {
        /** 连接失败 */
        CONNECTION_FAILED,
        /** 认证失败 */
        AUTH_FAILED,
        /** 执行超时 */
        TIMEOUT,
        /** 服务器错误 */
        SERVER_ERROR
    }

    public OpenClawException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public OpenClawException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}