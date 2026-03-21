package com.openclaw.workflow.controller;

import com.openclaw.workflow.dto.ApiResponse;
import com.openclaw.workflow.entity.PromptTemplate;
import com.openclaw.workflow.repository.PromptTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 提示词模板控制器
 */
@RestController
@RequestMapping("/api/prompt-templates")
public class PromptTemplateController {

    @Autowired
    private PromptTemplateRepository templateRepository;

    /**
     * 获取模板列表
     */
    @GetMapping
    public ResponseEntity<List<PromptTemplate>> list(@RequestParam(required = false) String type) {
        List<PromptTemplate> templates;
        if (type != null && !type.isEmpty()) {
            templates = templateRepository.findByType(type);
        } else {
            templates = templateRepository.findAll();
        }
        return ResponseEntity.ok(templates);
    }

    /**
     * 获取单个模板
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        return templateRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建模板
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody PromptTemplate template) {
        if (template.getId() == null || template.getId().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.badRequest("模板ID不能为空"));
        }
        if (template.getType() == null || template.getType().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.badRequest("模板类型不能为空"));
        }
        if (template.getContent() == null || template.getContent().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.badRequest("模板内容不能为空"));
        }

        PromptTemplate saved = templateRepository.save(template);
        return ResponseEntity.ok(saved);
    }

    /**
     * 更新模板
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody PromptTemplate template) {
        PromptTemplate existing = templateRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        if (template.getName() != null) {
            existing.setName(template.getName());
        }
        if (template.getContent() != null) {
            existing.setContent(template.getContent());
        }
        if (template.getVariables() != null) {
            existing.setVariables(template.getVariables());
        }

        PromptTemplate saved = templateRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    /**
     * 设置默认模板
     */
    @PostMapping("/{id}/set-default")
    public ResponseEntity<?> setDefault(@PathVariable String id) {
        PromptTemplate template = templateRepository.findById(id).orElse(null);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }

        // 清除同类型的其他默认
        templateRepository.clearDefaultByType(template.getType());

        // 设置当前为默认
        template.setDefault(true);
        templateRepository.save(template);

        return ResponseEntity.ok(ApiResponse.success(template));
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        if (!templateRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        templateRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}