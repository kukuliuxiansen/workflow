package com.openclaw.workflow.controller;

import com.openclaw.workflow.engine.smartdecompose.v2.template.TemplateVariable;
import com.openclaw.workflow.entity.TemplateConfig;
import com.openclaw.workflow.service.TemplateConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 模板配置控制器
 */
@RestController
@RequestMapping("/api/smart-templates")
public class TemplateConfigController {

    @Autowired
    private TemplateConfigService templateConfigService;

    private Map<String, Object> response(int code, String message, Object data, boolean success) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("message", message);
        map.put("data", data);
        map.put("success", success);
        return map;
    }

    /**
     * 获取模板列表
     */
    @GetMapping
    public ResponseEntity<?> list() {
        List<TemplateConfig> templates = templateConfigService.findAll();
        return ResponseEntity.ok(response(200, "success", templates, true));
    }

    /**
     * 获取模板详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        Optional<TemplateConfig> template = templateConfigService.findById(id);
        if (!template.isPresent()) {
            return ResponseEntity.ok(response(404, "模板不存在", null, false));
        }
        return ResponseEntity.ok(response(200, "success", template.get(), true));
    }

    /**
     * 获取默认模板
     */
    @GetMapping("/default")
    public ResponseEntity<?> getDefault() {
        Optional<TemplateConfig> template = templateConfigService.findDefault();
        return ResponseEntity.ok(response(200, "success", template.orElse(null), true));
    }

    /**
     * 创建模板
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody TemplateConfig template) {
        try {
            TemplateConfig created = templateConfigService.create(template);
            return ResponseEntity.ok(response(200, "创建成功", created, true));
        } catch (Exception e) {
            return ResponseEntity.ok(response(500, "创建失败: " + e.getMessage(), null, false));
        }
    }

    /**
     * 更新模板
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody TemplateConfig template) {
        try {
            TemplateConfig updated = templateConfigService.update(id, template);
            return ResponseEntity.ok(response(200, "更新成功", updated, true));
        } catch (Exception e) {
            return ResponseEntity.ok(response(500, "更新失败: " + e.getMessage(), null, false));
        }
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            templateConfigService.delete(id);
            return ResponseEntity.ok(response(200, "删除成功", null, true));
        } catch (Exception e) {
            return ResponseEntity.ok(response(500, "删除失败: " + e.getMessage(), null, false));
        }
    }

    /**
     * 设置默认模板
     */
    @PutMapping("/{id}/set-default")
    public ResponseEntity<?> setDefault(@PathVariable String id) {
        try {
            templateConfigService.setDefault(id);
            return ResponseEntity.ok(response(200, "设置成功", null, true));
        } catch (Exception e) {
            return ResponseEntity.ok(response(500, "设置失败: " + e.getMessage(), null, false));
        }
    }

    /**
     * 复制模板
     */
    @PostMapping("/{id}/copy")
    public ResponseEntity<?> copy(@PathVariable String id) {
        try {
            TemplateConfig copied = templateConfigService.copy(id);
            return ResponseEntity.ok(response(200, "复制成功", copied, true));
        } catch (Exception e) {
            return ResponseEntity.ok(response(500, "复制失败: " + e.getMessage(), null, false));
        }
    }

    /**
     * 获取变量列表
     */
    @GetMapping("/variables")
    public ResponseEntity<?> getVariables(@RequestParam(required = false, defaultValue = "decision") String type) {
        List<TemplateVariable.VariableInfo> variables = templateConfigService.getVariables(type);
        return ResponseEntity.ok(response(200, "success", variables, true));
    }

    /**
     * 预览模板
     */
    @PostMapping("/preview")
    public ResponseEntity<?> preview(@RequestBody Map<String, Object> request) {
        String template = (String) request.get("template");
        @SuppressWarnings("unchecked")
        Map<String, String> variables = (Map<String, String>) request.get("variables");

        String result = templateConfigService.preview(template, variables);
        return ResponseEntity.ok(response(200, "success", result, true));
    }
}