package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.TemplateConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 模板配置Repository
 */
@Repository
public interface TemplateConfigRepository extends JpaRepository<TemplateConfig, String> {

    /**
     * 查找默认模板
     */
    Optional<TemplateConfig> findByIsDefaultTrue();

    /**
     * 查找所有模板，按更新时间倒序
     */
    List<TemplateConfig> findAllByOrderByUpdatedAtDesc();

    /**
     * 检查名称是否存在
     */
    boolean existsByName(String name);
}