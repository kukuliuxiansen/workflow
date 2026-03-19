package com.openclaw.workflow.service;

import com.openclaw.workflow.entity.Workflow;
import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.repository.WorkflowEdgeRepository;
import com.openclaw.workflow.repository.WorkflowNodeRepository;
import com.openclaw.workflow.repository.WorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NodeService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;

    public NodeService(WorkflowRepository workflowRepository,
                       WorkflowNodeRepository nodeRepository,
                       WorkflowEdgeRepository edgeRepository) {
        this.workflowRepository = workflowRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
    }

    public List<WorkflowNode> findByWorkflowId(String workflowId) {
        return nodeRepository.findByWorkflowIdOrderByCreatedAtAsc(workflowId);
    }

    public WorkflowNode findById(String workflowId, String nodeId) {
        return nodeRepository.findByWorkflowIdAndId(workflowId, nodeId)
                .orElseThrow(() -> new RuntimeException("节点不存在: " + nodeId));
    }

    @Transactional
    public WorkflowNode create(String workflowId, WorkflowNode.NodeType type,
                                String name, Integer positionX, Integer positionY,
                                String config) {
        // 验证工作流是否存在
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("工作流不存在: " + workflowId));

        WorkflowNode node = new WorkflowNode();
        node.setId("node_" + UUID.randomUUID().toString().substring(0, 8));
        node.setWorkflowId(workflowId);
        node.setType(type != null ? type : WorkflowNode.NodeType.AGENT_EXECUTION);
        node.setName(name != null ? name : "新节点");
        node.setPositionX(positionX != null ? positionX : 300);
        node.setPositionY(positionY != null ? positionY : 150);
        node.setConfig(config);
        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());
        return nodeRepository.save(node);
    }

    @Transactional
    public WorkflowNode update(String workflowId, String nodeId,
                                String name, Integer positionX, Integer positionY,
                                String config) {
        WorkflowNode node = findById(workflowId, nodeId);
        if (name != null) node.setName(name);
        if (positionX != null) node.setPositionX(positionX);
        if (positionY != null) node.setPositionY(positionY);
        if (config != null) node.setConfig(config);
        node.setUpdatedAt(LocalDateTime.now());
        return nodeRepository.save(node);
    }

    @Transactional
    public void updatePosition(String workflowId, String nodeId, Integer x, Integer y) {
        WorkflowNode node = findById(workflowId, nodeId);
        node.setPositionX(x);
        node.setPositionY(y);
        node.setUpdatedAt(LocalDateTime.now());
        nodeRepository.save(node);
    }

    @Transactional
    public void delete(String workflowId, String nodeId) {
        // 删除相关的边
        edgeRepository.deleteBySourceNodeId(nodeId);
        edgeRepository.deleteByTargetNodeId(nodeId);
        // 删除节点
        nodeRepository.deleteById(nodeId);
    }
}