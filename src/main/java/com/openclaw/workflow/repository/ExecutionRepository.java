package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.Execution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionRepository extends JpaRepository<Execution, String> {

    List<Execution> findByWorkflowIdOrderByCreatedAtDesc(String workflowId);

    List<Execution> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT e FROM Execution e ORDER BY e.createdAt DESC")
    List<Execution> findAllByOrderByCreatedAtDesc();

    boolean existsByWorkflowIdAndStatusIn(String workflowId, List<String> statuses);
}