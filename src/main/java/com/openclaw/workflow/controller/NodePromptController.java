package com.openclaw.workflow.controller;

import com.openclaw.workflow.engine.config.NodePromptConfig;
import com.openclaw.workflow.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点提示词配置API
 *
 * 允许用户查看和定制各节点类型的默认提示词模板
 */
@RestController
@RequestMapping("/api/prompts")
public class NodePromptController {

    private final NodePromptConfig promptConfig;

    // 支持的节点类型
    private static final List<String> SUPPORTED_NODE_TYPES = Arrays.asList(
            "condition",      // 条件判断
            "parallel",       // 并行执行
            "loop",           // 循环
            "agent_execution",// Agent执行
            "human_review"    // 人工审核
    );

    public NodePromptController(NodePromptConfig promptConfig) {
        this.promptConfig = promptConfig;
    }

    /**
     * 获取所有支持的节点类型
     */
    @GetMapping("/node-types")
    public ApiResponse<List<NodeTypeInfo>> getNodeTypes() {
        List<NodeTypeInfo> types = new ArrayList<>();
        for (String type : SUPPORTED_NODE_TYPES) {
            NodeTypeInfo info = new NodeTypeInfo();
            info.type = type;
            info.name = getNodeTypeName(type);
            info.hasDefault = promptConfig.getPromptTemplate(type) != null && !promptConfig.getPromptTemplate(type).isEmpty();
            info.hasCustom = promptConfig.getAllCustomPrompts().containsKey(type);
            types.add(info);
        }
        return ApiResponse.success(types);
    }

    /**
     * 获取指定节点类型的提示词模板
     */
    @GetMapping("/{nodeType}")
    public ApiResponse<PromptTemplateInfo> getPromptTemplate(@PathVariable String nodeType) {
        if (!SUPPORTED_NODE_TYPES.contains(nodeType)) {
            return ApiResponse.badRequest("不支持的节点类型: " + nodeType);
        }

        PromptTemplateInfo info = new PromptTemplateInfo();
        info.nodeType = nodeType;
        info.name = getNodeTypeName(nodeType);
        info.defaultTemplate = promptConfig.getAllDefaultPrompts().get(nodeType);
        info.customTemplate = promptConfig.getAllCustomPrompts().get(nodeType);
        info.currentTemplate = promptConfig.getPromptTemplate(nodeType);
        info.variables = getTemplateVariables(nodeType);

        return ApiResponse.success(info);
    }

    /**
     * 获取所有提示词模板
     */
    @GetMapping("/all")
    public ApiResponse<Map<String, PromptTemplateInfo>> getAllPromptTemplates() {
        Map<String, PromptTemplateInfo> result = new HashMap<>();

        for (String nodeType : SUPPORTED_NODE_TYPES) {
            PromptTemplateInfo info = new PromptTemplateInfo();
            info.nodeType = nodeType;
            info.name = getNodeTypeName(nodeType);
            info.defaultTemplate = promptConfig.getAllDefaultPrompts().get(nodeType);
            info.customTemplate = promptConfig.getAllCustomPrompts().get(nodeType);
            info.currentTemplate = promptConfig.getPromptTemplate(nodeType);
            info.variables = getTemplateVariables(nodeType);
            result.put(nodeType, info);
        }

        return ApiResponse.success(result);
    }

    /**
     * 更新指定节点类型的自定义提示词模板
     */
    @PutMapping("/{nodeType}")
    public ApiResponse<String> updateCustomPrompt(
            @PathVariable String nodeType,
            @RequestBody UpdatePromptRequest request) {

        if (!SUPPORTED_NODE_TYPES.contains(nodeType)) {
            return ApiResponse.badRequest("不支持的节点类型: " + nodeType);
        }

        if (request.template == null || request.template.trim().isEmpty()) {
            return ApiResponse.badRequest("提示词模板不能为空");
        }

        promptConfig.setCustomPrompt(nodeType, request.template);
        return ApiResponse.success("提示词模板已更新");
    }

    /**
     * 重置指定节点类型的提示词模板为默认值
     */
    @DeleteMapping("/{nodeType}")
    public ApiResponse<String> resetToDefault(@PathVariable String nodeType) {
        if (!SUPPORTED_NODE_TYPES.contains(nodeType)) {
            return ApiResponse.badRequest("不支持的节点类型: " + nodeType);
        }

        promptConfig.resetToDefault(nodeType);
        return ApiResponse.success("已重置为默认提示词模板");
    }

    /**
     * 批量更新提示词模板
     */
    @PutMapping("/batch")
    public ApiResponse<String> batchUpdatePrompts(@RequestBody Map<String, String> prompts) {
        // 过滤只保留支持的节点类型
        Map<String, String> filtered = new HashMap<>();
        for (Map.Entry<String, String> entry : prompts.entrySet()) {
            if (SUPPORTED_NODE_TYPES.contains(entry.getKey())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }

        if (filtered.isEmpty()) {
            return ApiResponse.badRequest("没有有效的提示词模板");
        }

        promptConfig.setCustomPrompts(filtered);
        return ApiResponse.success("已更新 " + filtered.size() + " 个提示词模板");
    }

    // 辅助方法
    private String getNodeTypeName(String type) {
        switch (type) {
            case "condition":
                return "条件判断节点";
            case "parallel":
                return "并行执行节点";
            case "loop":
                return "循环节点";
            case "agent_execution":
                return "Agent执行节点";
            case "human_review":
                return "人工审核节点";
            default:
                return type;
        }
    }

    private List<TemplateVariable> getTemplateVariables(String nodeType) {
        switch (nodeType) {
            case "condition":
                return Arrays.asList(
                        new TemplateVariable("workflowId", "工作流ID", "系统自动注入"),
                        new TemplateVariable("executionId", "执行ID", "系统自动注入"),
                        new TemplateVariable("nodeId", "节点ID", "系统自动注入"),
                        new TemplateVariable("nodeName", "节点名称", "系统自动注入"),
                        new TemplateVariable("taskContext", "任务上下文", "系统自动注入"),
                        new TemplateVariable("upstreamOutputs", "上游节点输出", "系统自动获取"),
                        new TemplateVariable("downstreamNodes", "下游节点信息", "系统自动获取"),
                        new TemplateVariable("branchTable", "分支表格", "根据配置生成"),
                        new TemplateVariable("defaultBranchHint", "默认分支提示", "根据配置生成")
                );
            case "parallel":
                return Arrays.asList(
                        new TemplateVariable("workflowId", "工作流ID", "系统自动注入"),
                        new TemplateVariable("executionId", "执行ID", "系统自动注入"),
                        new TemplateVariable("nodeId", "节点ID", "系统自动注入"),
                        new TemplateVariable("nodeName", "节点名称", "系统自动注入"),
                        new TemplateVariable("executionMode", "执行模式", "ALL/DYNAMIC"),
                        new TemplateVariable("taskContext", "任务上下文", "系统自动注入"),
                        new TemplateVariable("upstreamOutputs", "上游节点输出", "系统自动获取"),
                        new TemplateVariable("downstreamNodes", "下游节点信息", "系统自动获取"),
                        new TemplateVariable("branchTable", "分支表格", "根据配置生成"),
                        new TemplateVariable("executionModeDesc", "执行模式说明", "根据模式生成"),
                        new TemplateVariable("decisionRequirements", "决策要求", "根据模式生成")
                );
            case "loop":
                return Arrays.asList(
                        new TemplateVariable("workflowId", "工作流ID", "系统自动注入"),
                        new TemplateVariable("executionId", "执行ID", "系统自动注入"),
                        new TemplateVariable("nodeId", "节点ID", "系统自动注入"),
                        new TemplateVariable("nodeName", "节点名称", "系统自动注入"),
                        new TemplateVariable("loopMode", "循环模式", "iterator/condition"),
                        new TemplateVariable("currentIteration", "当前迭代次数", "系统自动追踪"),
                        new TemplateVariable("maxIterations", "最大迭代次数", "从配置读取"),
                        new TemplateVariable("loopVariable", "循环变量名", "从配置读取"),
                        new TemplateVariable("currentValue", "当前循环值", "系统自动获取"),
                        new TemplateVariable("progressBar", "进度条", "系统自动生成"),
                        new TemplateVariable("exitCondition", "退出条件", "从配置读取"),
                        new TemplateVariable("loopResults", "循环执行结果", "系统自动获取")
                );
            case "agent_execution":
                return Arrays.asList(
                        new TemplateVariable("workflowId", "工作流ID", "系统自动注入"),
                        new TemplateVariable("executionId", "执行ID", "系统自动注入"),
                        new TemplateVariable("nodeId", "节点ID", "系统自动注入"),
                        new TemplateVariable("nodeName", "节点名称", "系统自动注入"),
                        new TemplateVariable("taskDescription", "任务描述", "从执行输入读取"),
                        new TemplateVariable("globalPrompt", "全局提示", "从工作流配置读取"),
                        new TemplateVariable("nodePrompt", "节点提示", "从节点配置读取"),
                        new TemplateVariable("upstreamOutputs", "上游节点输出", "系统自动获取"),
                        new TemplateVariable("downstreamNodes", "下游节点信息", "系统自动获取")
                );
            default:
                return new ArrayList<>();
        }
    }

    // DTO类
    public static class NodeTypeInfo {
        public String type;
        public String name;
        public boolean hasDefault;
        public boolean hasCustom;
    }

    public static class PromptTemplateInfo {
        public String nodeType;
        public String name;
        public String defaultTemplate;
        public String customTemplate;
        public String currentTemplate;
        public List<TemplateVariable> variables;
    }

    public static class TemplateVariable {
        public String name;
        public String description;
        public String source;

        public TemplateVariable(String name, String description, String source) {
            this.name = name;
            this.description = description;
            this.source = source;
        }
    }

    public static class UpdatePromptRequest {
        public String template;
    }
}