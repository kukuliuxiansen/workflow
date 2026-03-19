package com.openclaw.workflow.engine;

import com.openclaw.workflow.engine.model.NodeResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 执行上下文管理器
 */
@Component
public class ContextManager {

    private Map<String, Object> input = new HashMap<>();
    private Map<String, NodeResult> nodeOutputs = new HashMap<>();
    private Map<String, Object> variables = new HashMap<>();
    private String previousNodeId;
    private String taskName;
    private String taskDescription;
    private String projectPath;
    private String globalPrompt;

    public ContextManager() {}

    /**
     * 初始化上下文
     */
    public void init(Map<String, Object> input, Map<String, Object> workflowInput) {
        if (input != null) {
            this.input.putAll(input);
        }
        if (workflowInput != null) {
            this.input.putAll(workflowInput);
        }

        // 提取常用字段
        if (this.input.containsKey("task_description")) {
            this.taskDescription = (String) this.input.get("task_description");
        }
        if (this.input.containsKey("task_name")) {
            this.taskName = (String) this.input.get("task_name");
        }
        if (this.input.containsKey("project_path")) {
            this.projectPath = (String) this.input.get("project_path");
        }
    }

    /**
     * 保存节点输出
     */
    public void saveNodeOutput(String nodeId, NodeResult result) {
        nodeOutputs.put(nodeId, result);
    }

    /**
     * 获取节点输出
     */
    public NodeResult getNodeOutput(String nodeId) {
        return nodeOutputs.get(nodeId);
    }

    /**
     * 获取所有输出
     */
    public Map<String, NodeResult> getAllOutputs() {
        return new HashMap<>(nodeOutputs);
    }

    /**
     * 获取输入
     */
    public Map<String, Object> getInput() {
        return new HashMap<>(input);
    }

    /**
     * 设置变量
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    /**
     * 获取变量
     */
    public Object getVariable(String key) {
        return variables.get(key);
    }

    // Getters and Setters
    public String getPreviousNodeId() { return previousNodeId; }
    public void setPreviousNodeId(String previousNodeId) { this.previousNodeId = previousNodeId; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getTaskDescription() { return taskDescription; }
    public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }

    public String getProjectPath() { return projectPath; }
    public void setProjectPath(String projectPath) { this.projectPath = projectPath; }

    public String getGlobalPrompt() { return globalPrompt; }
    public void setGlobalPrompt(String globalPrompt) { this.globalPrompt = globalPrompt; }
}