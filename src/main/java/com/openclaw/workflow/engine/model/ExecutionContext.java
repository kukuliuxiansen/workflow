package com.openclaw.workflow.engine.model;

import com.openclaw.workflow.entity.WorkflowNode;
import java.util.Map;

/**
 * 执行上下文
 */
public class ExecutionContext {

    private WorkflowNode node;
    private String workflowId;
    private String workflowName;
    private String executionId;
    private Map<String, Object> input;
    private Map<String, NodeResult> previousOutputs;
    private String taskDescription;
    private String projectPath;

    public ExecutionContext() {}

    // Getters and Setters
    public WorkflowNode getNode() { return node; }
    public void setNode(WorkflowNode node) { this.node = node; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public Map<String, Object> getInput() { return input; }
    public void setInput(Map<String, Object> input) { this.input = input; }

    public Map<String, NodeResult> getPreviousOutputs() { return previousOutputs; }
    public void setPreviousOutputs(Map<String, NodeResult> previousOutputs) { this.previousOutputs = previousOutputs; }

    public String getTaskDescription() { return taskDescription; }
    public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }

    public String getProjectPath() { return projectPath; }
    public void setProjectPath(String projectPath) { this.projectPath = projectPath; }
}