package com.openclaw.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.openclaw.workflow.dto.WorkflowDto;
import com.openclaw.workflow.entity.Workflow;
import com.openclaw.workflow.entity.WorkflowEdge;
import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.repository.WorkflowEdgeRepository;
import com.openclaw.workflow.repository.WorkflowNodeRepository;
import com.openclaw.workflow.repository.WorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public WorkflowService(WorkflowRepository workflowRepository,
                           WorkflowNodeRepository nodeRepository,
                           WorkflowEdgeRepository edgeRepository) {
        this.workflowRepository = workflowRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
    }

    public List<Workflow> findAll() {
        return workflowRepository.findAll();
    }

    public List<Workflow> findByFolderId(String folderId) {
        return workflowRepository.findByFolderIdOrderBySortOrderAsc(folderId);
    }

    public Workflow findById(String id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("工作流不存在: " + id));
    }

    @Transactional
    public Workflow create(String name, String description, String folderId) {
        Workflow workflow = new Workflow();
        workflow.setId("wf_" + System.currentTimeMillis());
        workflow.setName(name != null ? name : "新建工作流");
        workflow.setDescription(description);
        workflow.setFolderId(folderId);
        workflow.setStatus(Workflow.WorkflowStatus.DRAFT);
        workflow.setVersion("1.0");
        workflow.setCreatedAt(LocalDateTime.now());
        workflow.setUpdatedAt(LocalDateTime.now());

        // 创建开始节点
        WorkflowNode startNode = new WorkflowNode();
        startNode.setId("start");
        startNode.setWorkflowId(workflow.getId());
        startNode.setType(WorkflowNode.NodeType.START);
        startNode.setName("开始");
        startNode.setPositionX(100);
        startNode.setPositionY(150);
        startNode.setCreatedAt(LocalDateTime.now());
        nodeRepository.save(startNode);

        // 创建结束节点
        WorkflowNode finishNode = new WorkflowNode();
        finishNode.setId("finish");
        finishNode.setWorkflowId(workflow.getId());
        finishNode.setType(WorkflowNode.NodeType.FINISH);
        finishNode.setName("结束");
        finishNode.setPositionX(500);
        finishNode.setPositionY(150);
        finishNode.setCreatedAt(LocalDateTime.now());
        nodeRepository.save(finishNode);

        return workflowRepository.save(workflow);
    }

    @Transactional
    public Workflow update(String id, String name, String description, String globalConfig, String taskConfig) {
        Workflow workflow = findById(id);
        if (name != null) workflow.setName(name);
        if (description != null) workflow.setDescription(description);
        if (globalConfig != null) workflow.setGlobalConfig(globalConfig);
        if (taskConfig != null) workflow.setTaskConfig(taskConfig);
        workflow.setUpdatedAt(LocalDateTime.now());
        return workflowRepository.save(workflow);
    }

    /**
     * 更新工作流，同时保存节点和边
     */
    @Transactional
    public Workflow updateWithNodesAndEdges(String id, String name, String description, String globalConfig,
                                            String taskConfig, List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        Workflow workflow = findById(id);

        // 更新工作流基本信息
        if (name != null) workflow.setName(name);
        if (description != null) workflow.setDescription(description);
        if (globalConfig != null) workflow.setGlobalConfig(globalConfig);
        if (taskConfig != null) workflow.setTaskConfig(taskConfig);
        workflow.setUpdatedAt(LocalDateTime.now());
        workflowRepository.save(workflow);

        // 保存节点和边
        if (nodes != null) {
            saveNodes(id, nodes);
        }
        if (edges != null) {
            saveEdges(id, edges);
        }

        return workflow;
    }

    /**
     * 保存节点列表
     */
    private void saveNodes(String workflowId, List<WorkflowNode> nodes) {
        for (WorkflowNode node : nodes) {
            if (node.getWorkflowId() == null) {
                node.setWorkflowId(workflowId);
            }
            if (node.getCreatedAt() == null) {
                node.setCreatedAt(LocalDateTime.now());
            }
            node.setUpdatedAt(LocalDateTime.now());
            nodeRepository.save(node);
        }
    }

    /**
     * 保存边列表
     */
    private void saveEdges(String workflowId, List<WorkflowEdge> edges) {
        for (WorkflowEdge edge : edges) {
            if (edge.getWorkflowId() == null) {
                edge.setWorkflowId(workflowId);
            }
            if (edge.getCreatedAt() == null) {
                edge.setCreatedAt(LocalDateTime.now());
            }
            edgeRepository.save(edge);
        }
    }

    @Transactional
    public void delete(String id) {
        nodeRepository.deleteByWorkflowId(id);
        edgeRepository.deleteByWorkflowId(id);
        workflowRepository.deleteById(id);
    }

    @Transactional
    public Workflow clone(String id) {
        Workflow original = findById(id);
        List<WorkflowNode> nodes = nodeRepository.findByWorkflowIdOrderByCreatedAtAsc(id);
        List<WorkflowEdge> edges = edgeRepository.findByWorkflowId(id);

        // 创建新的工作流
        Workflow cloned = new Workflow();
        cloned.setId("wf_" + System.currentTimeMillis());
        cloned.setName(original.getName() + " (副本)");
        cloned.setDescription(original.getDescription());
        cloned.setFolderId(original.getFolderId());
        cloned.setStatus(Workflow.WorkflowStatus.DRAFT);
        cloned.setVersion(original.getVersion());
        cloned.setGlobalConfig(original.getGlobalConfig());
        cloned.setCreatedAt(LocalDateTime.now());
        cloned.setUpdatedAt(LocalDateTime.now());
        workflowRepository.save(cloned);

        // 克隆节点 - 为所有节点生成新ID，避免主键冲突
        Map<String, String> nodeIdMap = new HashMap<>();
        for (WorkflowNode node : nodes) {
            // 所有节点都生成新ID，包括start和finish
            String newId = "node_" + UUID.randomUUID().toString().substring(0, 8);
            nodeIdMap.put(node.getId(), newId);

            WorkflowNode clonedNode = new WorkflowNode();
            clonedNode.setId(newId);
            clonedNode.setWorkflowId(cloned.getId());
            clonedNode.setType(node.getType());
            clonedNode.setName(node.getName());
            clonedNode.setDescription(node.getDescription());
            clonedNode.setPositionX(node.getPositionX());
            clonedNode.setPositionY(node.getPositionY());
            clonedNode.setConfig(node.getConfig());
            clonedNode.setCreatedAt(LocalDateTime.now());
            nodeRepository.save(clonedNode);
        }

        // 克隆边 - 跳过无效的边
        for (WorkflowEdge edge : edges) {
            String newSourceId = nodeIdMap.get(edge.getSourceNodeId());
            String newTargetId = nodeIdMap.get(edge.getTargetNodeId());

            // 跳过源或目标节点不存在的边
            if (newSourceId == null || newTargetId == null) {
                continue;
            }

            WorkflowEdge clonedEdge = new WorkflowEdge();
            clonedEdge.setId("edge_" + UUID.randomUUID().toString().substring(0, 8));
            clonedEdge.setWorkflowId(cloned.getId());
            clonedEdge.setSourceNodeId(newSourceId);
            clonedEdge.setTargetNodeId(newTargetId);
            clonedEdge.setEdgeType(edge.getEdgeType());
            clonedEdge.setLabel(edge.getLabel());
            edgeRepository.save(clonedEdge);
        }

        return cloned;
    }

    @Transactional
    public void move(String id, String targetFolderId) {
        Workflow workflow = findById(id);
        workflow.setFolderId(targetFolderId);
        workflow.setUpdatedAt(LocalDateTime.now());
        workflowRepository.save(workflow);
    }

    public String exportYaml(String id) {
        Workflow workflow = findById(id);
        List<WorkflowNode> nodes = nodeRepository.findByWorkflowIdOrderByCreatedAtAsc(id);
        List<WorkflowEdge> edges = edgeRepository.findByWorkflowId(id);

        Map<String, Object> yamlData = new LinkedHashMap<>();
        Map<String, Object> workflowData = new LinkedHashMap<>();
        workflowData.put("id", workflow.getId());
        workflowData.put("name", workflow.getName());
        workflowData.put("version", workflow.getVersion());
        workflowData.put("description", workflow.getDescription());
        yamlData.put("workflow", workflowData);

        List<Map<String, Object>> nodesList = new ArrayList<>();
        for (WorkflowNode node : nodes) {
            Map<String, Object> nodeData = new LinkedHashMap<>();
            nodeData.put("id", node.getId());
            nodeData.put("type", node.getType().name().toLowerCase());
            nodeData.put("name", node.getName());
            nodeData.put("position_x", node.getPositionX());
            nodeData.put("position_y", node.getPositionY());
            if (node.getConfig() != null) {
                try {
                    nodeData.put("config", objectMapper.readValue(node.getConfig(), Map.class));
                } catch (JsonProcessingException e) {
                    nodeData.put("config", node.getConfig());
                }
            }
            nodesList.add(nodeData);
        }
        yamlData.put("nodes", nodesList);

        List<Map<String, Object>> edgesList = new ArrayList<>();
        for (WorkflowEdge edge : edges) {
            Map<String, Object> edgeData = new LinkedHashMap<>();
            edgeData.put("source", edge.getSourceNodeId());
            edgeData.put("target", edge.getTargetNodeId());
            edgeData.put("type", edge.getEdgeType().name().toLowerCase());
            edgesList.add(edgeData);
        }
        yamlData.put("edges", edgesList);

        try {
            return yamlMapper.writeValueAsString(yamlData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("导出YAML失败", e);
        }
    }
}