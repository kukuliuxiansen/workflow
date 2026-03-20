package com.openclaw.workflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.connector.OpenClawGatewayClient;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.engine.service.NodePromptService;
import com.openclaw.workflow.engine.util.AgentDecisionParser;
import com.openclaw.workflow.engine.util.NodePromptBuilder;
import com.openclaw.workflow.entity.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 条件判断节点处理器
 */
public class ConditionNodeHandler extends BaseNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConditionNodeHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String gatewayUrl = "http://localhost:18789";
    private String gatewayToken = "56b640cc2d91411f63255af68355c19ee33c88ec458878ca";
    private String decisionAgentId = "project-manager";
    private NodePromptService promptService;

    @Override
    public NodeResult execute(NodeExecutionContext context) throws Exception {
        WorkflowNode node = context.getCurrentNode();
        ConditionConfig config = parseConfig(node);

        Branch selectedBranch;
        String reason;

        if ("expression".equals(config.decisionMode)) {
            selectedBranch = evaluateByExpression(config, context);
            reason = "表达式匹配";
        } else {
            selectedBranch = evaluateByAgent(node, config, context);
            reason = "Agent决策";
        }

        if (selectedBranch == null) {
            selectedBranch = findBranchById(config.branches, config.defaultBranch);
            reason = "使用默认分支";
        }

        if (selectedBranch == null) {
            return NodeResult.failure("没有可用的分支");
        }

        return NodeResult.success(
                String.format("选择分支: %s。原因: %s", selectedBranch.name, reason),
                selectedBranch.targetNodeId
        );
    }

    private Branch evaluateByExpression(ConditionConfig config, NodeExecutionContext context) {
        for (Branch branch : config.branches) {
            if (branch.conditionExpr != null && !branch.conditionExpr.isEmpty()) {
                if (ConditionEvaluator.evaluate(branch.conditionExpr, context)) {
                    return branch;
                }
            }
        }
        return null;
    }

    private Branch evaluateByAgent(WorkflowNode node, ConditionConfig config,
                                   NodeExecutionContext context) throws Exception {
        String prompt = buildPrompt(node, config, context);

        OpenClawGatewayClient client = new OpenClawGatewayClient(gatewayUrl, gatewayToken);
        String sessionContext = String.format("%s_%s_%s_condition",
                context.getWorkflowId(), context.getExecutionId(), node.getId());

        OpenClawGatewayClient.AgentRequest request = OpenClawGatewayClient.AgentRequest.builder()
                .agentId(decisionAgentId)
                .message(prompt)
                .context(sessionContext)
                .build();

        logger.info("条件判断Agent调用: {} - 提示词长度: {}", node.getName(), prompt.length());

        OpenClawGatewayClient.AgentResponse response = client.executeAgent(request);

        if (!response.isSuccess()) {
            logger.error("决策Agent执行失败: {}", response.getErrorMessage());
            return null;
        }

        AgentDecisionParser.AgentDecision decision = AgentDecisionParser.parse(response.getContent());

        if (decision != null && decision.getFirstNodeId() != null) {
            String selectedId = decision.getFirstNodeId();
            Branch branch = findBranchById(config.branches, selectedId);
            if (branch != null) {
                return branch;
            }
            logger.warn("Agent返回了无效的分支ID: {}", selectedId);
        }

        return null;
    }

    private String buildPrompt(WorkflowNode node, ConditionConfig config, NodeExecutionContext context) {
        if (promptService != null) {
            List<NodePromptService.BranchInfo> branchInfos = new ArrayList<>();
            for (Branch branch : config.branches) {
                branchInfos.add(new NodePromptService.BranchInfo(
                        branch.id, branch.name, branch.description, branch.conditionDesc));
            }
            return promptService.buildConditionPrompt(
                    context.getWorkflowId(), context.getExecutionId(), node.getId(), node.getName(),
                    branchInfos, config.defaultBranch, context.getPreviousOutputs(),
                    context.getTaskDescription(), config.customPrompt);
        }

        List<NodePromptBuilder.BranchInfo> branchInfos = new ArrayList<>();
        for (Branch branch : config.branches) {
            branchInfos.add(new NodePromptBuilder.BranchInfo(
                    branch.id, branch.name, branch.description, branch.conditionDesc));
        }
        return NodePromptBuilder.buildConditionPrompt(
                context.getWorkflowId(), context.getExecutionId(), node.getId(), node.getName(),
                branchInfos, config.defaultBranch, context.getPreviousOutputs(),
                context.getTaskDescription(), config.customPrompt);
    }

    private Branch findBranchById(List<Branch> branches, String id) {
        if (id == null) return null;
        for (Branch branch : branches) {
            if (id.equals(branch.id)) return branch;
        }
        return null;
    }

    private ConditionConfig parseConfig(WorkflowNode node) {
        ConditionConfig config = new ConditionConfig();
        config.decisionMode = "agent";

        try {
            if (node.getConfig() != null) {
                JsonNode jsonConfig = objectMapper.readTree(node.getConfig());

                if (jsonConfig.has("decisionMode")) {
                    config.decisionMode = jsonConfig.get("decisionMode").asText();
                }
                if (jsonConfig.has("expression")) {
                    config.expression = jsonConfig.get("expression").asText();
                }
                if (jsonConfig.has("defaultBranch")) {
                    config.defaultBranch = jsonConfig.get("defaultBranch").asText();
                }
                if (jsonConfig.has("customPrompt")) {
                    config.customPrompt = jsonConfig.get("customPrompt").asText();
                }

                if (jsonConfig.has("branches")) {
                    JsonNode branchesNode = jsonConfig.get("branches");
                    for (JsonNode branchNode : branchesNode) {
                        Branch branch = new Branch();
                        branch.id = branchNode.has("id") ? branchNode.get("id").asText() : null;
                        branch.name = branchNode.has("name") ? branchNode.get("name").asText() : null;
                        branch.description = branchNode.has("description") ? branchNode.get("description").asText() : null;
                        branch.targetNodeId = branchNode.has("targetNodeId") ? branchNode.get("targetNodeId").asText() : null;
                        branch.conditionExpr = branchNode.has("conditionExpr") ? branchNode.get("conditionExpr").asText() : null;
                        branch.conditionDesc = branchNode.has("conditionDesc") ? branchNode.get("conditionDesc").asText() : null;
                        config.branches.add(branch);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("解析条件配置失败: {}", e.getMessage());
        }

        return config;
    }

    // 内部类
    private static class ConditionConfig {
        String decisionMode = "agent";
        String expression;
        List<Branch> branches = new ArrayList<>();
        String defaultBranch;
        String customPrompt;
    }

    private static class Branch {
        String id;
        String name;
        String description;
        String targetNodeId;
        String conditionExpr;
        String conditionDesc;
    }

    // Setter方法
    public void setGatewayUrl(String gatewayUrl) { this.gatewayUrl = gatewayUrl; }
    public void setGatewayToken(String gatewayToken) { this.gatewayToken = gatewayToken; }
    public void setDecisionAgentId(String decisionAgentId) { this.decisionAgentId = decisionAgentId; }
    public void setPromptService(NodePromptService promptService) { this.promptService = promptService; }
}