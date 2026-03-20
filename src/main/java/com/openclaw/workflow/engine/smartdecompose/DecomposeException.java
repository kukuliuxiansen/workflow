package com.openclaw.workflow.engine.smartdecompose;

/**
 * 智能分解异常
 */
public class DecomposeException extends RuntimeException {

    private final DecomposeExceptionType type;
    private final String details;

    public DecomposeException(DecomposeExceptionType type) {
        super(type.getMessage());
        this.type = type;
        this.details = null;
    }

    public DecomposeException(DecomposeExceptionType type, String details) {
        super(type.getMessage() + ": " + details);
        this.type = type;
        this.details = details;
    }

    public DecomposeException(DecomposeExceptionType type, Throwable cause) {
        super(type.getMessage(), cause);
        this.type = type;
        this.details = cause.getMessage();
    }

    public DecomposeException(DecomposeExceptionType type, String details, Throwable cause) {
        super(type.getMessage() + ": " + details, cause);
        this.type = type;
        this.details = details;
    }

    public DecomposeExceptionType getType() {
        return type;
    }

    public String getDetails() {
        return details;
    }
}