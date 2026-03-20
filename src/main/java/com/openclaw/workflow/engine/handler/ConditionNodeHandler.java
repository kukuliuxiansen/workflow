package com.openclaw.workflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.connector.OpenClawGatewayClient;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
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
 *
 * 支持两种模式：
 * 1. 表达式模式：传统的表达式判断（==, !=）
 * 2. Agent决策模式：通过Agent分析上下文选择分支
 *
 * Agent决策协议：
 * [NODE_DECISION]
 * node_ids: branch_id
 * reason: 选择原因
 * [/NODE_DECISION]
 *
 * 配置结构：
 * {
 *   "decisionMode": "agent",
 *   "branches": [
 *     {
 *       "id": "branch_1",
 *       "name": "分支名称",
 *       "description": "分支描述",
 *       "targetNodeId": "node_xxx",
 *       "conditionExpr": "表达式模式的条件",
 *       "conditionDesc": "Agent模式的条件说明（给Agent看）"
 *     }
 *   ],
 *   "defaultBranch": "branch_1",
 *   "customPrompt": "自定义提示词（可选）"
 * }
 */
public class ConditionNodeHandler extends BaseNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConditionNodeHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Gateway API配置
    private String gatewayUrl = "http://localhost:18789";
    private String gatewayToken = "56b640cc2d91411f63255af68355c19ee33c88ec458878ca";

    // 决策Agent配置
    private String decisionAgentId = "project-manager";

    @Override
    public NodeResult execute(NodeExecutionContext context) throws Exception {
        WorkflowNode node = context.getNode();
        ConditionConfig config = parseConfig(node);

        logger.info("执行条件判断节点: {} (模式: {})", node.getName(), config.decisionMode);

        List<Branch> branches = config.branches;
        if (branches.isEmpty()) {
            logger.warn("条件节点没有配置分支");
            return NodeResult.failed("条件节点没有配置分支");
        }

        String selectedBranchId;
        String reason;

        if ("expression".equalsIgnoreCase(config.decisionMode)) {
            // 表达式模式
            ExpressionResult result = evaluateByExpression(config, context);
            selectedBranchId = result.branchId;
            reason = result.reason;
        } else {
            // Agent决策模式
            AgentDecisionResult result = evaluateByAgent(node, config, context);
            selectedBranchId = result.branchId;
            reason = result.reason;
        }

        // 查找选中的分支
        Branch selectedBranch = null;
        for (Branch branch : branches) {
            if (branch.id.equals(selectedBranchId)) {
                selectedBranch = branch;
                break;
            }
        }

        // 如果没有匹配，使用默认分支
        if (selectedBranch == null) {
            logger.warn("未找到匹配的分支: {}, 使用默认分支", selectedBranchId);
            selectedBranchId = config.defaultBranch;
            for (Branch branch : branches) {
                if (branch.id.equals(selectedBranchId)) {
                    selectedBranch = branch;
                    break;
                }
            }
        }

        // 如果仍然没有找到，使用第一个分支
        if (selectedBranch == null && !branches.isEmpty()) {
            selectedBranch = branches.get(0);
            selectedBranchId = selectedBranch.id;
        }

        logger.info("条件判断结果: {} -> {} ({})", node.getName(), selectedBranchId, reason);

        return NodeResult.success(
                String.format("选择分支: %s。原因: %s", selectedBranch.name, reason),
                selectedBranch.targetNodeId
        );
    }

    /**
     * 表达式模式判断
     */
    private ExpressionResult evaluateByExpression(ConditionConfig config, NodeExecutionContext context) {
        ExpressionResult result = new ExpressionResult();

        String expression = config.expression;
        if (expression == null || expression.isEmpty()) {
            result.branchId = config.defaultBranch;
            result.reason = "未配置表达式，使用默认分支";
            return result;
        }

        // 解析表达式（支持简单的 == 和 !=）
        for (Branch branch : config.branches) {
            if (branch.conditionExpr != null && !branch.conditionExpr.isEmpty()) {
                if (evaluateCondition(branch.conditionExpr, context)) {
                    result.branchId = branch.id;
                    result.reason = String.format("表达式 '%s' 匹配", branch.conditionExpr);
                    return result;
                }
            }
        }

        // 检查全局表达式
        if (evaluateCondition(expression, context)) {
            // 查找第一个分支
            result.branchId = config.branches.isEmpty() ? config.defaultBranch : config.branches.get(0).id;
            result.reason = "条件满足";
        } else {
            result.branchId = config.defaultBranch;
            result.reason = "条件不满足，使用默认分支";
        }

        return result;
    }

    /**
     * Agent决策模式判断
     */
    private AgentDecisionResult evaluateByAgent(WorkflowNode node, ConditionConfig config,
                                                  NodeExecutionContext context) throws Exception {
        AgentDecisionResult result = new AgentDecisionResult();

        // 构建分支信息列表
        List<NodePromptBuilder.BranchInfo> branchInfos = new ArrayList<>();
        for (Branch branch : config.branches) {
            branchInfos.add(new NodePromptBuilder.BranchInfo(
                    branch.id,
                    branch.name,
                    branch.description,
                    branch.conditionDesc  // 条件描述（给Agent看）
            ));
        }

        // 使用通用的提示词构建器
        String prompt = NodePromptBuilder.buildConditionPrompt(
                context.getWorkflowId(),
                context.getExecutionId(),
                node.getId(),
                node.getName(),
                branchInfos,
                config.defaultBranch,
                context.getPreviousOutputs(),
                context.getTaskDescription(),
                config.customPrompt
        );

        // 调用Agent获取决策
        OpenClawGatewayClient client = new OpenClawGatewayClient(gatewayUrl, gatewayToken);
        String sessionContext = String.format("%s_%s_%s_condition",
                context.getWorkflowId(),
                context.getExecutionId(),
                node.getId());

        OpenClawGatewayClient.AgentRequest request = OpenClawGatewayClient.AgentRequest.builder()
                .agentId(decisionAgentId)
                .message(prompt)
                .context(sessionContext)
                .build();

        logger.info("条件判断Agent调用: {} - 提示词长度: {}", node.getName(), prompt.length());

        OpenClawGatewayClient.AgentResponse response = client.executeAgent(request);

        if (!response.isSuccess()) {
            logger.error("决策Agent执行失败: {}", response.getErrorMessage());
            result.branchId = config.defaultBranch;
            result.reason = "Agent执行失败，使用默认分支";
            return result;
        }

        // 解析决策
        AgentDecisionParser.AgentDecision decision = AgentDecisionParser.parse(response.getContent());

        // 验证决策
        List<String> validBranchIds = new ArrayList<>();
        for (Branch branch : config.branches) {
            validBranchIds.add(branch.id);
        }

        if (decision != null && decision.getNodeIds() != null && !decision.getNodeIds().isEmpty()) {
            String selectedId = decision.getFirstNodeId();
            if (validBranchIds.contains(selectedId)) {
                result.branchId = selectedId;
                result.reason = decision.getReason() != null ? decision.getReason() : "Agent决策";
            } else {
                logger.warn("Agent返回了无效的分支ID: {}", selectedId);
                result.branchId = config.defaultBranch;
                result.reason = "Agent返回了无效的分支，使用默认分支";
            }
        } else {
            result.branchId = config.defaultBranch;
            result.reason = "Agent未返回有效决策，使用默认分支";
        }

        return result;
    }

    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(String expression, NodeExecutionContext context) {
        if (expression == null || expression.isEmpty()) {
            return true;
        }

        expression = expression.trim();

        if (expression.contains("==")) {
            String[] parts = expression.split("==");
            if (parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim().replace("\"", "").replace("'", "");
                Object leftValue = getVariableValue(left, context);
                return String.valueOf(leftValue).equals(right);
            }
        } else if (expression.contains("!=")) {
            String[] parts = expression.split("!=");
            if (parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim().replace("\"", "").replace("'", "");
                Object leftValue = getVariableValue(left, context);
                return !String.valueOf(leftValue).equals(right);
            }
        } else if (expression.contains(">=")) {
            String[] parts = expression.split(">=");
            if (parts.length == 2) {
                try {
                    double left = Double.parseDouble(getVariableValue(parts[0].trim(), context).toString());
                    double right = Double.parseDouble(parts[1].trim());
                    return left >= right;
                } catch (Exception e) {
                    return false;
                }
            }
        } else if (expression.contains("<=")) {
            String[] parts = expression.split("<=");
            if (parts.length == 2) {
                try {
                    double left = Double.parseDouble(getVariableValue(parts[0].trim(), context).toString());
                    double right = Double.parseDouble(parts[1].trim());
                    return left <= right;
                } catch (Exception e) {
                    return false;
                }
            }
        } else if (expression.contains(">")) {
            String[] parts = expression.split(">");
            if (parts.length == 2) {
                try {
                    double left = Double.parseDouble(getVariableValue(parts[0].trim(), context).toString());
                    double right = Double.parseDouble(parts[1].trim());
                    return left > right;
                } catch (Exception e) {
                    return false;
                }
            }
        } else if (expression.contains("<")) {
            String[] parts = expression.split("<");
            if (parts.length == 2) {
                try {
                    double left = Double.parseDouble(getVariableValue(parts[0].trim(), context).toString());
                    double right = Double.parseDouble(parts[1].trim());
                    return left < right;
                } catch (Exception e) {
                    return false;
                }
            }
        }

        return true;
    }

    private Object getVariableValue(String varName, NodeExecutionContext context) {
        if (context.getInput() != null && context.getInput().containsKey(varName)) {
            return context.getInput().get(varName);
        }

        if (context.getPreviousOutputs() != null) {
            for (Map.Entry<String, NodeResult> entry : context.getPreviousOutputs().entrySet()) {
                if (entry.getValue() != null && entry.getValue().getOutput() != null) {
                    // 如果变量名匹配节点ID
                    if (entry.getKey().equals(varName)) {
                        return entry.getValue().getOutput();
                    }
                    // 检查输出是否是Map且包含该变量
                    Object output = entry.getValue().getOutput();
                    if (output instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) output;
                        if (map.containsKey(varName)) {
                            return map.get(varName);
                        }
                    }
                }
            }
        }

        return varName;
    }

    /**
     * 解析节点配置
     */
    private ConditionConfig parseConfig(WorkflowNode node) {
        ConditionConfig config = new ConditionConfig();
        config.decisionMode = "agent"; // 默认使用Agent决策模式

        try {
            if (node.getConfig() != null) {
                JsonNode jsonConfig = objectMapper.readTree(node.getConfig());

                // 解析决策模式
                if (jsonConfig.has("decisionMode")) {
                    config.decisionMode = jsonConfig.get("decisionMode").asText();
                }

                // 解析表达式
                if (jsonConfig.has("expression")) {
                    config.expression = jsonConfig.get("expression").asText();
                }

                // 解析默认分支
                if (jsonConfig.has("defaultBranch")) {
                    config.defaultBranch = jsonConfig.get("defaultBranch").asText();
                }

                // 解析自定义提示词
                if (jsonConfig.has("customPrompt")) {
                    config.customPrompt = jsonConfig.get("customPrompt").asText();
                }

                // 解析分支
                if (jsonConfig.has("branches") && jsonConfig.get("branches").isArray()) {
                    for (JsonNode branchNode : jsonConfig.get("branches")) {
                        Branch branch = new Branch();
                        branch.id = branchNode.has("id") ? branchNode.get("id").asText() : "";
                        branch.name = branchNode.has("name") ? branchNode.get("name").asText() : "";
                        branch.description = branchNode.has("description") ? branchNode.get("description").asText() : "";
                        branch.targetNodeId = branchNode.has("targetNodeId") ? branchNode.get("targetNodeId").asText() : "";
                        branch.conditionExpr = branchNode.has("conditionExpr") ? branchNode.get("conditionExpr").asText() : null;
                        // 新增：条件描述（给Agent看）
                        branch.conditionDesc = branchNode.has("conditionDesc") ? branchNode.get("conditionDesc").asText() : null;
                        config.branches.add(branch);
                    }
                }

                // 解析决策Agent
                if (jsonConfig.has("decisionAgentId")) {
                    decisionAgentId = jsonConfig.get("decisionAgentId").asText();
                }
            }
        } catch (Exception e) {
            logger.error("解析条件节点配置失败: {}", e.getMessage(), e);
        }

        // 如果没有设置默认分支，使用第一个分支
        if (config.defaultBranch == null && !config.branches.isEmpty()) {
            config.defaultBranch = config.branches.get(0).id;
        }

        return config;
    }

    // ==================== 内部类 ====================

    private static class ConditionConfig {
        String decisionMode = "agent";
        String expression;
        String defaultBranch;
        List<Branch> branches = new ArrayList<>();
        String customPrompt;  // 自定义提示词
    }

    private static class Branch {
        String id;
        String name;
        String description;
        String targetNodeId;
        String conditionExpr;   // 表达式模式的条件
        String conditionDesc;   // Agent模式的条件说明（给Agent看）
    }

    private static class ExpressionResult {
        String branchId;
        String reason;
    }

    private static class AgentDecisionResult {
        String branchId;
        String reason;
    }

    // ==================== 配置方法 ====================

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public void setGatewayToken(String gatewayToken) {
        this.gatewayToken = gatewayToken;
    }

    public void setDecisionAgentId(String decisionAgentId) {
        this.decisionAgentId = decisionAgentId;
    }
}