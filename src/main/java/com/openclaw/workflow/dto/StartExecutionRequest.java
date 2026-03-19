package com.openclaw.workflow.dto;

import java.util.Map;

/**
 * 启动执行请求
 */
public class StartExecutionRequest {

    private Map<String, Object> inputData;

    private String taskDescription;

    public Map<String, Object> getInputData() { return inputData; }
    public void setInputData(Map<String, Object> inputData) { this.inputData = inputData; }

    public String getTaskDescription() { return taskDescription; }
    public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }
}