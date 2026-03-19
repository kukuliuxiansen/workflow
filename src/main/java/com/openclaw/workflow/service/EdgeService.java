package com.openclaw.workflow.service;

import com.openclaw.workflow.entity.WorkflowEdge;
import com.openclaw.workflow.repository.WorkflowEdgeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EdgeService {

    private final WorkflowEdgeRepository edgeRepository;

    public EdgeService(WorkflowEdgeRepository edgeRepository) {
        this.edgeRepository = edgeRepository;
    }

    public List<WorkflowEdge> findByWorkflowId(String workflowId) {
        return edgeRepository.findByWorkflowId(workflowId);
    }

    @Transactional
    public WorkflowEdge create(String workflowId, String sourceNodeId,
                                String targetNodeId, WorkflowEdge.EdgeType edgeType,
                                String label) {
        WorkflowEdge edge = new WorkflowEdge();
        edge.setId("edge_" + UUID.randomUUID().toString().substring(0, 8));
        edge.setWorkflowId(workflowId);
        edge.setSourceNodeId(sourceNodeId);
        edge.setTargetNodeId(targetNodeId);
        edge.setEdgeType(edgeType != null ? edgeType : WorkflowEdge.EdgeType.SUCCESS);
        edge.setLabel(label);
        return edgeRepository.save(edge);
    }

    @Transactional
    public void delete(String workflowId, String edgeId) {
        edgeRepository.deleteById(edgeId);
    }
}