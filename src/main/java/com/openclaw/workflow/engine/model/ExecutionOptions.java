package com.openclaw.workflow.engine.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 执行选项
 */
public class ExecutionOptions {

    private String executionId;
    private boolean resetRetry;
    private int maxRetries = 3;
    private int maxGlobalRetries = 10;
    private int maxGlobalLoop = 100;
    private int timeout = 600;
    private Map<String, Object> extraOptions = new HashMap<>();

    public ExecutionOptions() {}

    // Getters and Setters
    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public boolean isResetRetry() { return resetRetry; }
    public void setResetRetry(boolean resetRetry) { this.resetRetry = resetRetry; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public int getMaxGlobalRetries() { return maxGlobalRetries; }
    public void setMaxGlobalRetries(int maxGlobalRetries) { this.maxGlobalRetries = maxGlobalRetries; }

    public int getMaxGlobalLoop() { return maxGlobalLoop; }
    public void setMaxGlobalLoop(int maxGlobalLoop) { this.maxGlobalLoop = maxGlobalLoop; }

    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }

    public Map<String, Object> getExtraOptions() { return extraOptions; }
    public void setExtraOptions(Map<String, Object> extraOptions) { this.extraOptions = extraOptions; }

    public Object getExtraOption(String key) {
        return extraOptions.get(key);
    }

    public void setExtraOption(String key, Object value) {
        extraOptions.put(key, value);
    }
}