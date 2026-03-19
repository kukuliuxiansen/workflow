package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.TaskConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskConfigRepository extends JpaRepository<TaskConfig, String> {

    Optional<TaskConfig> findByExecutionId(String executionId);

    void deleteByExecutionId(String executionId);
}