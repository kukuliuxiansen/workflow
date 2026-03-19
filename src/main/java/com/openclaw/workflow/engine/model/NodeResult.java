package com.openclaw.workflow.engine.model;

/**
 * 节点执行结果
 */
public class NodeResult {

    private String status; // success, failed, retry
    private Object output;
    private String error;
    private boolean waitingRetry;

    public NodeResult() {}

    public static NodeResult success(Object output) {
        NodeResult result = new NodeResult();
        result.setStatus("success");
        result.setOutput(output);
        return result;
    }

    public static NodeResult failed(String error) {
        NodeResult result = new NodeResult();
        result.setStatus("failed");
        result.setError(error);
        return result;
    }

    public static NodeResult failed(Object output, String error) {
        NodeResult result = new NodeResult();
        result.setStatus("failed");
        result.setOutput(output);
        result.setError(error);
        return result;
    }

    public static NodeResult retry() {
        NodeResult result = new NodeResult();
        result.setStatus("retry");
        return result;
    }

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Object getOutput() { return output; }
    public void setOutput(Object output) { this.output = output; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public boolean isWaitingRetry() { return waitingRetry; }
    public void setWaitingRetry(boolean waitingRetry) { this.waitingRetry = waitingRetry; }
}