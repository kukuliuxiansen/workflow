package com.openclaw.workflow.service;

import com.openclaw.workflow.engine.smartdecompose.v2.template.TemplateVariable;
import com.openclaw.workflow.entity.TemplateConfig;
import com.openclaw.workflow.repository.TemplateConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 模板配置服务
 */
@Service
public class TemplateConfigService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateConfigService.class);

    @Autowired
    private TemplateConfigRepository templateConfigRepository;

    /**
     * 获取所有模板
     */
    public List<TemplateConfig> findAll() {
        return templateConfigRepository.findAllByOrderByUpdatedAtDesc();
    }

    /**
     * 获取模板详情
     */
    public Optional<TemplateConfig> findById(String id) {
        return templateConfigRepository.findById(id);
    }

    /**
     * 获取默认模板
     */
    public Optional<TemplateConfig> findDefault() {
        return templateConfigRepository.findByIsDefaultTrue();
    }

    /**
     * 创建模板
     */
    @Transactional
    public TemplateConfig create(TemplateConfig template) {
        if (template.getId() == null || template.getId().isEmpty()) {
            template.setId("tpl_" + System.currentTimeMillis());
        }
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        if (template.getIsDefault() == null) {
            template.setIsDefault(false);
        }

        if (Boolean.TRUE.equals(template.getIsDefault())) {
            clearDefaultFlag();
        }

        return templateConfigRepository.save(template);
    }

    /**
     * 更新模板
     */
    @Transactional
    public TemplateConfig update(String id, TemplateConfig template) {
        Optional<TemplateConfig> existing = templateConfigRepository.findById(id);
        if (!existing.isPresent()) {
            throw new RuntimeException("模板不存在: " + id);
        }

        TemplateConfig entity = existing.get();
        entity.setName(template.getName());
        entity.setDecisionTemplate(template.getDecisionTemplate());
        entity.setReviewTemplate(template.getReviewTemplate());
        entity.setRetryTemplate(template.getRetryTemplate());
        entity.setUpdatedAt(LocalDateTime.now());

        if (Boolean.TRUE.equals(template.getIsDefault()) && !Boolean.TRUE.equals(entity.getIsDefault())) {
            clearDefaultFlag();
            entity.setIsDefault(true);
        }

        return templateConfigRepository.save(entity);
    }

    /**
     * 删除模板
     */
    @Transactional
    public void delete(String id) {
        Optional<TemplateConfig> template = templateConfigRepository.findById(id);
        if (!template.isPresent()) {
            throw new RuntimeException("模板不存在: " + id);
        }
        if (Boolean.TRUE.equals(template.get().getIsDefault())) {
            throw new RuntimeException("默认模板不可删除");
        }
        templateConfigRepository.deleteById(id);
    }

    /**
     * 设置默认模板
     */
    @Transactional
    public void setDefault(String id) {
        Optional<TemplateConfig> template = templateConfigRepository.findById(id);
        if (!template.isPresent()) {
            throw new RuntimeException("模板不存在: " + id);
        }

        clearDefaultFlag();

        TemplateConfig entity = template.get();
        entity.setIsDefault(true);
        entity.setUpdatedAt(LocalDateTime.now());
        templateConfigRepository.save(entity);
    }

    /**
     * 复制模板
     */
    @Transactional
    public TemplateConfig copy(String id) {
        Optional<TemplateConfig> existing = templateConfigRepository.findById(id);
        if (!existing.isPresent()) {
            throw new RuntimeException("模板不存在: " + id);
        }

        TemplateConfig source = existing.get();
        TemplateConfig copy = new TemplateConfig();
        copy.setId("tpl_" + System.currentTimeMillis());
        copy.setName(source.getName() + " (副本)");
        copy.setDecisionTemplate(source.getDecisionTemplate());
        copy.setReviewTemplate(source.getReviewTemplate());
        copy.setRetryTemplate(source.getRetryTemplate());
        copy.setIsDefault(false);
        copy.setCreatedAt(LocalDateTime.now());
        copy.setUpdatedAt(LocalDateTime.now());

        return templateConfigRepository.save(copy);
    }

    /**
     * 获取变量列表
     */
    public List<TemplateVariable.VariableInfo> getVariables(String type) {
        TemplateVariable.TemplateType templateType;
        switch (type.toLowerCase()) {
            case "decision":
                templateType = TemplateVariable.TemplateType.DECISION;
                break;
            case "review":
                templateType = TemplateVariable.TemplateType.REVIEW;
                break;
            case "retry":
                templateType = TemplateVariable.TemplateType.RETRY;
                break;
            default:
                templateType = TemplateVariable.TemplateType.DECISION;
        }
        return TemplateVariable.getVariableInfoList(templateType);
    }

    /**
     * 预览模板（替换变量）
     */
    public String preview(String template, Map<String, String> variables) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        String result = template;
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        return result;
    }

    /**
     * 清除所有默认标记
     */
    private void clearDefaultFlag() {
        Optional<TemplateConfig> defaultTemplate = templateConfigRepository.findByIsDefaultTrue();
        if (defaultTemplate.isPresent()) {
            TemplateConfig entity = defaultTemplate.get();
            entity.setIsDefault(false);
            templateConfigRepository.save(entity);
        }
    }
}