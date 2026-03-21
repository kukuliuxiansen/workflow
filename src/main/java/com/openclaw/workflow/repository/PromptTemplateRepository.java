package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 提示词模板 Repository
 */
@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, String> {

    /**
     * 按类型查询模板列表
     */
    List<PromptTemplate> findByType(String type);

    /**
     * 查询指定类型的默认模板
     */
    Optional<PromptTemplate> findByTypeAndIsDefaultTrue(String type);

    /**
     * 清除指定类型的默认标记
     */
    @Modifying
    @Query("UPDATE PromptTemplate t SET t.isDefault = false WHERE t.type = ?1")
    void clearDefaultByType(String type);

    /**
     * 查询所有默认模板
     */
    List<PromptTemplate> findByIsDefaultTrue();
}