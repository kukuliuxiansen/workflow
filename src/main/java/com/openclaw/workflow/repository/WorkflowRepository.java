package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, String> {

    List<Workflow> findByFolderIdOrderBySortOrderAsc(String folderId);

    List<Workflow> findByFolderIdIsNullOrderBySortOrderAsc();

    List<Workflow> findByStatus(Workflow.WorkflowStatus status);

    @Query("SELECT w FROM Workflow w ORDER BY w.sortOrder ASC, w.updatedAt DESC")
    List<Workflow> findAllOrderBySortOrderAsc();

    long countByFolderId(String folderId);
}