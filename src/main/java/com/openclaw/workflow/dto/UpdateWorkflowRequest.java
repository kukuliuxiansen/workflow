package com.openclaw.workflow.dto;

import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.entity.WorkflowEdge;

import java.util.List;

/**
 * 更新工作流请求
 */
public class UpdateWorkflowRequest {

    private String name;
    private String description;
    private String globalConfig;
    private String taskConfig;

    // 支持同时保存节点和边
    private List<WorkflowNode> nodes;
    private List<WorkflowEdge> edges;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGlobalConfig() { return globalConfig; }
    public void setGlobalConfig(String globalConfig) { this.globalConfig = globalConfig; }

    public String getTaskConfig() { return taskConfig; }
    public void setTaskConfig(String taskConfig) { this.taskConfig = taskConfig; }

    public List<WorkflowNode> getNodes() { return nodes; }
    public void setNodes(List<WorkflowNode> nodes) { this.nodes = nodes; }

    public List<WorkflowEdge> getEdges() { return edges; }
    public void setEdges(List<WorkflowEdge> edges) { this.edges = edges; }
}