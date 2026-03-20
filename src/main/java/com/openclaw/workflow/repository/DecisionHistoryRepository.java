package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.DecisionHistory;
import com.openclaw.workflow.entity.DecisionHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 决策历史Repository
 */
@Repository
public interface DecisionHistoryRepository extends JpaRepository<DecisionHistory, DecisionHistoryId> {

    List<DecisionHistory> findByExecutionIdAndNodeIdOrderByIterationAsc(String executionId, String nodeId);

    List<DecisionHistory> findTop10ByExecutionIdAndNodeIdOrderByIterationDesc(String executionId, String nodeId);

    void deleteByExecutionId(String executionId);
}