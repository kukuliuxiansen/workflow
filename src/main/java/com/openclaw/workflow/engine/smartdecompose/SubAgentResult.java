package com.openclaw.workflow.engine.smartdecompose;

import java.util.Map;

/**
 * 子Agent执行结果
 */
public class SubAgentResult {

    private boolean success;
    private String status;      // success, failed, timeout
    private String summary;
    private Map<String, Object> outputs;
    private String errorMessage;
    private int totalTokens;
    private long durationMs;

    public static SubAgentResult success(String summary, Map<String, Object> outputs) {
        SubAgentResult result = new SubAgentResult();
        result.setSuccess(true);
        result.setStatus("success");
        result.setSummary(summary);
        result.setOutputs(outputs);
        return result;
    }

    public static SubAgentResult error(String errorMessage) {
        SubAgentResult result = new SubAgentResult();
        result.setSuccess(false);
        result.setStatus("failed");
        result.setErrorMessage(errorMessage);
        return result;
    }

    public static SubAgentResult timeout(String summary) {
        SubAgentResult result = new SubAgentResult();
        result.setSuccess(false);
        result.setStatus("timeout");
        result.setSummary(summary);
        return result;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Map<String, Object> getOutputs() { return outputs; }
    public void setOutputs(Map<String, Object> outputs) { this.outputs = outputs; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
}