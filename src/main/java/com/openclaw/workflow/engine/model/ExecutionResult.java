package com.openclaw.workflow.engine.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 执行结果
 */
public class ExecutionResult {

    private boolean success;
    private String executionId;
    private String error;
    private Map<String, Object> outputs = new HashMap<>();

    public ExecutionResult() {}

    public static ExecutionResult success(String executionId, Map<String, Object> outputs) {
        ExecutionResult result = new ExecutionResult();
        result.setSuccess(true);
        result.setExecutionId(executionId);
        result.setOutputs(outputs);
        return result;
    }

    public static ExecutionResult failed(String executionId, String error) {
        ExecutionResult result = new ExecutionResult();
        result.setSuccess(false);
        result.setExecutionId(executionId);
        result.setError(error);
        return result;
    }

    public static ExecutionResult paused(String executionId, String error) {
        ExecutionResult result = new ExecutionResult();
        result.setSuccess(false);
        result.setExecutionId(executionId);
        result.setError(error);
        return result;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public Map<String, Object> getOutputs() { return outputs; }
    public void setOutputs(Map<String, Object> outputs) { this.outputs = outputs; }
}