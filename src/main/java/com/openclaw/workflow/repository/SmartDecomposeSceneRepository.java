package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.SmartDecomposeScene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * SmartDecompose 场景配置 Repository
 */
@Repository
public interface SmartDecomposeSceneRepository extends JpaRepository<SmartDecomposeScene, String> {

    /**
     * 查找默认场景
     */
    Optional<SmartDecomposeScene> findByIsDefaultTrue();

    /**
     * 查找所有场景，按名称排序
     */
    List<SmartDecomposeScene> findAllByOrderByNameAsc();

    /**
     * 按名称查找
     */
    Optional<SmartDecomposeScene> findByName(String name);
}