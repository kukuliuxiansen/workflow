package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.WorkflowNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowNodeRepository extends JpaRepository<WorkflowNode, String> {

    List<WorkflowNode> findByWorkflowIdOrderByCreatedAtAsc(String workflowId);

    Optional<WorkflowNode> findByWorkflowIdAndId(String workflowId, String id);

    void deleteByWorkflowId(String workflowId);

    long countByWorkflowId(String workflowId);
}