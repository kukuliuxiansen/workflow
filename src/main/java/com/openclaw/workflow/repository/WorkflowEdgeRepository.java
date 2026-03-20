package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.WorkflowEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowEdgeRepository extends JpaRepository<WorkflowEdge, String> {

    List<WorkflowEdge> findByWorkflowId(String workflowId);

    List<WorkflowEdge> findBySourceNodeId(String sourceNodeId);

    List<WorkflowEdge> findByTargetNodeId(String targetNodeId);

    List<WorkflowEdge> findByWorkflowIdAndSourceNodeId(String workflowId, String sourceNodeId);

    List<WorkflowEdge> findByWorkflowIdAndTargetNodeId(String workflowId, String targetNodeId);

    Optional<WorkflowEdge> findByWorkflowIdAndId(String workflowId, String id);

    void deleteByWorkflowId(String workflowId);

    void deleteBySourceNodeId(String sourceNodeId);

    void deleteByTargetNodeId(String targetNodeId);
}