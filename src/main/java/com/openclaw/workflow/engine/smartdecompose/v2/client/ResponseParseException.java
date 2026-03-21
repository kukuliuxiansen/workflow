package com.openclaw.workflow.engine.smartdecompose.v2.client;

/**
 * 响应解析异常
 *
 * 当无法从 OpenClaw 响应中提取或解析 JSON 时抛出。
 */
public class ResponseParseException extends RuntimeException {

    public ResponseParseException(String message) {
        super(message);
    }

    public ResponseParseException(String message, Throwable cause) {
        super(message, cause);
    }
}