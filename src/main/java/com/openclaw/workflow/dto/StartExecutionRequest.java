package com.openclaw.workflow.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * 启动执行请求
 */
public class StartExecutionRequest {

    @JsonAlias("input")
    private Map<String, Object> inputData;
    private String taskDescription;

    @JsonProperty("taskConfig")  // 显式指定JSON字段名，覆盖SNAKE_CASE策略
    @JsonAlias("task_config")    // 同时支持snake_case
    private Map<String, Object> taskConfig;

    public Map<String, Object> getInputData() { return inputData; }
    public void setInputData(Map<String, Object> inputData) { this.inputData = inputData; }

    public String getTaskDescription() {
        // 优先使用直接字段
        if (taskDescription != null) {
            return taskDescription;
        }
        // 从 inputData 中提取
        if (inputData != null && inputData.containsKey("task_description")) {
            return (String) inputData.get("task_description");
        }
        return null;
    }
    public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }

    public Map<String, Object> getTaskConfig() { return taskConfig; }
    public void setTaskConfig(Map<String, Object> taskConfig) { this.taskConfig = taskConfig; }

    // 便捷方法：获取全局提示词（支持两种命名格式）
    public String getGlobalPrompt() {
        if (taskConfig != null) {
            if (taskConfig.containsKey("globalPrompt")) {
                return (String) taskConfig.get("globalPrompt");
            }
            if (taskConfig.containsKey("global_prompt")) {
                return (String) taskConfig.get("global_prompt");
            }
        }
        return null;
    }

    // 便捷方法：获取项目路径（支持两种命名格式）
    public String getProjectPath() {
        if (taskConfig != null) {
            if (taskConfig.containsKey("projectPath")) {
                return (String) taskConfig.get("projectPath");
            }
            if (taskConfig.containsKey("project_path")) {
                return (String) taskConfig.get("project_path");
            }
        }
        return null;
    }
}