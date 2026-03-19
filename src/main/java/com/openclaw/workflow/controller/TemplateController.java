package com.openclaw.workflow.controller;

import com.openclaw.workflow.dto.ApiResponse;
import com.openclaw.workflow.entity.Workflow;
import com.openclaw.workflow.repository.WorkflowRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "Template", description = "模板管理接口")
@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final WorkflowRepository workflowRepository;

    public TemplateController(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    // 内置模板
    private static final List<Map<String, Object>> BUILTIN_TEMPLATES = Arrays.asList(
        createTemplate("tpl-001", "基础工作流", "包含开始和结束节点的简单工作流",
            "[{\"id\":\"start-1\",\"type\":\"start\",\"position\":{\"x\":100,\"y\":100},\"data\":{\"label\":\"开始\"}}," +
            "{\"id\":\"finish-1\",\"type\":\"finish\",\"position\":{\"x\":400,\"y\":100},\"data\":{\"label\":\"结束\"}}]",
            "[{\"id\":\"edge-1\",\"source\":\"start-1\",\"target\":\"finish-1\",\"type\":\"success\"}]"),

        createTemplate("tpl-002", "Agent执行流程", "包含Agent执行节点的工作流",
            "[{\"id\":\"start-1\",\"type\":\"start\",\"position\":{\"x\":100,\"y\":100},\"data\":{\"label\":\"开始\"}}," +
            "{\"id\":\"agent-1\",\"type\":\"agent_execution\",\"position\":{\"x\":300,\"y\":100},\"data\":{\"label\":\"Agent执行\",\"config\":{\"agentId\":\"\",\"prompt\":\"\"}}}," +
            "{\"id\":\"finish-1\",\"type\":\"finish\",\"position\":{\"x\":500,\"y\":100},\"data\":{\"label\":\"结束\"}}]",
            "[{\"id\":\"edge-1\",\"source\":\"start-1\",\"target\":\"agent-1\",\"type\":\"success\"}," +
            "{\"id\":\"edge-2\",\"source\":\"agent-1\",\"target\":\"finish-1\",\"type\":\"success\"}]"),

        createTemplate("tpl-003", "条件分支流程", "包含条件判断的工作流",
            "[{\"id\":\"start-1\",\"type\":\"start\",\"position\":{\"x\":100,\"y\":100},\"data\":{\"label\":\"开始\"}}," +
            "{\"id\":\"condition-1\",\"type\":\"condition\",\"position\":{\"x\":300,\"y\":100},\"data\":{\"label\":\"条件判断\",\"config\":{\"expression\":\"\"}}}," +
            "{\"id\":\"agent-1\",\"type\":\"agent_execution\",\"position\":{\"x\":500,\"y\":50},\"data\":{\"label\":\"分支A\"}}," +
            "{\"id\":\"agent-2\",\"type\":\"agent_execution\",\"position\":{\"x\":500,\"y\":150},\"data\":{\"label\":\"分支B\"}}," +
            "{\"id\":\"finish-1\",\"type\":\"finish\",\"position\":{\"x\":700,\"y\":100},\"data\":{\"label\":\"结束\"}}]",
            "[{\"id\":\"edge-1\",\"source\":\"start-1\",\"target\":\"condition-1\",\"type\":\"success\"}," +
            "{\"id\":\"edge-2\",\"source\":\"condition-1\",\"target\":\"agent-1\",\"type\":\"success\"}," +
            "{\"id\":\"edge-3\",\"source\":\"condition-1\",\"target\":\"agent-2\",\"type\":\"fail\"}," +
            "{\"id\":\"edge-4\",\"source\":\"agent-1\",\"target\":\"finish-1\",\"type\":\"success\"}," +
            "{\"id\":\"edge-5\",\"source\":\"agent-2\",\"target\":\"finish-1\",\"type\":\"success\"}]"),

        createTemplate("tpl-004", "人工审核流程", "包含人工审核节点的流程",
            "[{\"id\":\"start-1\",\"type\":\"start\",\"position\":{\"x\":100,\"y\":100},\"data\":{\"label\":\"开始\"}}," +
            "{\"id\":\"agent-1\",\"type\":\"agent_execution\",\"position\":{\"x\":300,\"y\":100},\"data\":{\"label\":\"自动执行\"}}," +
            "{\"id\":\"review-1\",\"type\":\"human_review\",\"position\":{\"x\":500,\"y\":100},\"data\":{\"label\":\"人工审核\"}}," +
            "{\"id\":\"finish-1\",\"type\":\"finish\",\"position\":{\"x\":700,\"y\":100},\"data\":{\"label\":\"结束\"}}]",
            "[{\"id\":\"edge-1\",\"source\":\"start-1\",\"target\":\"agent-1\",\"type\":\"success\"}," +
            "{\"id\":\"edge-2\",\"source\":\"agent-1\",\"target\":\"review-1\",\"type\":\"success\"}," +
            "{\"id\":\"edge-3\",\"source\":\"review-1\",\"target\":\"finish-1\",\"type\":\"success\"}]")
    );

    private static Map<String, Object> createTemplate(String id, String name, String description,
                                                       String nodes, String edges) {
        Map<String, Object> template = new HashMap<>();
        template.put("id", id);
        template.put("name", name);
        template.put("description", description);
        template.put("nodes", nodes);
        template.put("edges", edges);
        template.put("builtin", true);
        return template;
    }

    @Operation(summary = "获取模板列表")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        List<Map<String, Object>> templates = new ArrayList<>(BUILTIN_TEMPLATES);

        // 添加已保存的工作流作为模板
        List<Workflow> workflows = workflowRepository.findByStatus(Workflow.WorkflowStatus.DRAFT);
        for (Workflow w : workflows) {
            Map<String, Object> template = new HashMap<>();
            template.put("id", "wf-" + w.getId());
            template.put("name", w.getName());
            template.put("description", w.getDescription() != null ? w.getDescription() : "");
            template.put("builtin", false);
            template.put("workflowId", w.getId());
            templates.add(template);
        }

        return ApiResponse.success(templates);
    }

    @Operation(summary = "获取模板详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable String id) {
        // 查找内置模板
        for (Map<String, Object> template : BUILTIN_TEMPLATES) {
            if (template.get("id").equals(id)) {
                return ApiResponse.success(template);
            }
        }

        // 查找工作流模板
        if (id.startsWith("wf-")) {
            String workflowId = id.substring(3);
            Workflow workflow = workflowRepository.findById(workflowId)
                    .orElseThrow(() -> new RuntimeException("模板不存在: " + id));

            Map<String, Object> template = new HashMap<>();
            template.put("id", id);
            template.put("name", workflow.getName());
            template.put("description", workflow.getDescription() != null ? workflow.getDescription() : "");
            template.put("builtin", false);
            template.put("workflowId", workflowId);
            return ApiResponse.success(template);
        }

        throw new RuntimeException("模板不存在: " + id);
    }

    @Operation(summary = "从模板创建工作流")
    @PostMapping("/{id}/create-workflow")
    public ApiResponse<Workflow> createFromTemplate(@PathVariable String id,
                                                     @RequestBody(required = false) Map<String, String> request) {
        String name = request != null ? request.get("name") : null;

        // 查找内置模板
        for (Map<String, Object> template : BUILTIN_TEMPLATES) {
            if (template.get("id").equals(id)) {
                Workflow workflow = new Workflow();
                workflow.setId("wf-" + System.currentTimeMillis());
                workflow.setName(name != null ? name : (String) template.get("name"));
                workflow.setDescription((String) template.get("description"));
                workflow.setVersion("1.0");
                workflow.setStatus(Workflow.WorkflowStatus.DRAFT);
                workflow.setCreatedAt(java.time.LocalDateTime.now());
                workflow.setUpdatedAt(java.time.LocalDateTime.now());
                workflow.setConfig((String) template.get("nodes"));
                workflow.setGlobalConfig((String) template.get("edges"));
                return ApiResponse.success(workflowRepository.save(workflow));
            }
        }

        throw new RuntimeException("模板不存在: " + id);
    }
}