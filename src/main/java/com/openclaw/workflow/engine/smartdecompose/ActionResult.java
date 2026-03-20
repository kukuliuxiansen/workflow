package com.openclaw.workflow.engine.smartdecompose;

import java.util.List;

/**
 * 动作执行结果
 */
public class ActionResult {

    private boolean success;            // 是否成功
    private String message;             // 结果消息
    private Object data;                // 返回数据
    private List<String> nextHints;     // 建议的下一步
    private boolean shouldContinue;     // 是否应继续循环
    private String waitingFor;          // 等待的事件（如user_input）

    public ActionResult() {
        this.shouldContinue = true;
    }

    public static ActionResult success(String message) {
        ActionResult result = new ActionResult();
        result.setSuccess(true);
        result.setMessage(message);
        return result;
    }

    public static ActionResult success(String message, Object data) {
        ActionResult result = success(message);
        result.setData(data);
        return result;
    }

    public static ActionResult success(String message, Object data, boolean shouldContinue) {
        ActionResult result = success(message, data);
        result.setShouldContinue(shouldContinue);
        return result;
    }

    public static ActionResult error(String message) {
        ActionResult result = new ActionResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    public static ActionResult error(String message, Object data, boolean shouldContinue) {
        ActionResult result = error(message);
        result.setData(data);
        result.setShouldContinue(shouldContinue);
        return result;
    }

    public static ActionResult waiting(String message, String waitingFor) {
        ActionResult result = new ActionResult();
        result.setSuccess(true);
        result.setMessage(message);
        result.setWaitingFor(waitingFor);
        result.setShouldContinue(false);
        return result;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public List<String> getNextHints() { return nextHints; }
    public void setNextHints(List<String> nextHints) { this.nextHints = nextHints; }

    public boolean isShouldContinue() { return shouldContinue; }
    public void setShouldContinue(boolean shouldContinue) { this.shouldContinue = shouldContinue; }

    public String getWaitingFor() { return waitingFor; }
    public void setWaitingFor(String waitingFor) { this.waitingFor = waitingFor; }
}