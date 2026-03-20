package com.openclaw.workflow.dto;

import com.openclaw.workflow.entity.Workflow;
import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.entity.WorkflowEdge;

import java.util.List;

/**
 * 工作流详情DTO（包含节点和边）
 */
public class WorkflowDto {

    private String id;
    private String name;
    private String description;
    private String version;
    private Workflow.WorkflowStatus status;
    private String folderId;
    private String globalConfig;
    private String taskConfig;
    private List<WorkflowNode> nodes;
    private List<WorkflowEdge> edges;

    public WorkflowDto() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public Workflow.WorkflowStatus getStatus() { return status; }
    public void setStatus(Workflow.WorkflowStatus status) { this.status = status; }

    public String getFolderId() { return folderId; }
    public void setFolderId(String folderId) { this.folderId = folderId; }

    public String getGlobalConfig() { return globalConfig; }
    public void setGlobalConfig(String globalConfig) { this.globalConfig = globalConfig; }

    public String getTaskConfig() { return taskConfig; }
    public void setTaskConfig(String taskConfig) { this.taskConfig = taskConfig; }

    public List<WorkflowNode> getNodes() { return nodes; }
    public void setNodes(List<WorkflowNode> nodes) { this.nodes = nodes; }

    public List<WorkflowEdge> getEdges() { return edges; }
    public void setEdges(List<WorkflowEdge> edges) { this.edges = edges; }
}