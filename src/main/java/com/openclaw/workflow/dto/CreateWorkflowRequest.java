package com.openclaw.workflow.dto;

import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.entity.WorkflowEdge;

import java.util.List;

/**
 * 创建工作流请求
 */
public class CreateWorkflowRequest {

    private String name;
    private String description;
    private String folderId;

    // 支持创建时同时保存节点和边
    private List<WorkflowNode> nodes;
    private List<WorkflowEdge> edges;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFolderId() { return folderId; }
    public void setFolderId(String folderId) { this.folderId = folderId; }

    public List<WorkflowNode> getNodes() { return nodes; }
    public void setNodes(List<WorkflowNode> nodes) { this.nodes = nodes; }

    public List<WorkflowEdge> getEdges() { return edges; }
    public void setEdges(List<WorkflowEdge> edges) { this.edges = edges; }
}