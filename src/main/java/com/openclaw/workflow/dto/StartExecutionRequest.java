package com.openclaw.workflow.dto;

import java.util.Map;

/**
 * 启动执行请求
 */
public class StartExecutionRequest {

    private Map<String, Object> inputData;
    private String taskDescription;
    private Map<String, Object> taskConfig;

    public Map<String, Object> getInputData() { return inputData; }
    public void setInputData(Map<String, Object> inputData) { this.inputData = inputData; }

    public String getTaskDescription() { return taskDescription; }
    public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }

    public Map<String, Object> getTaskConfig() { return taskConfig; }
    public void setTaskConfig(Map<String, Object> taskConfig) { this.taskConfig = taskConfig; }

    // 便捷方法：获取全局提示词
    public String getGlobalPrompt() {
        if (taskConfig != null && taskConfig.containsKey("globalPrompt")) {
            return (String) taskConfig.get("globalPrompt");
        }
        return null;
    }

    // 便捷方法：获取项目路径
    public String getProjectPath() {
        if (taskConfig != null && taskConfig.containsKey("projectPath")) {
            return (String) taskConfig.get("projectPath");
        }
        return null;
    }
}