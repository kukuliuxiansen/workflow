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

/**
 * 并行执行节点处理器
 *
 * 支持两种执行模式：
 * 1. ALL: 执行所有分支
 * 2. DYNAMIC: Agent选择执行哪些分支
 *
 * Agent决策协议：
 * [NODE_DECISION]
 * node_ids: branch_a, branch_b
 * reason: 选择原因
 * [/NODE_DECISION]
 *
 * 配置结构：
 * {
 *   "executionMode": "DYNAMIC",
 *   "branches": [
 *     {
 *       "id": "branch_1",
 *       "name": "分支名称",
 *       "description": "分支描述",
 *       "targetNodeId": "node_xxx",
 *       "conditionDesc": "执行条件说明（给Agent看）"
 *     }
 *   ],
 *   "mergeNode": "node_merge",
 *   "defaultBranches": ["branch_1"],
 *   "customPrompt": "自定义提示词（可选）"
 * }
 */
public class ParallelNodeHandler extends BaseNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(ParallelNodeHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Gateway API配置
    private String gatewayUrl = "http://localhost:18789";
    private String gatewayToken = "56b640cc2d91411f63255af68355c19ee33c88ec458878ca";

    // 决策Agent配置
    private String decisionAgentId = "project-manager";

    @Override
    public NodeResult execute(NodeExecutionContext context) throws Exception {
        WorkflowNode node = context.getNode();

        // 解析节点配置
        ParallelConfig config = parseConfig(node);

        logger.info("执行并行节点: {} (模式: {})", node.getName(), config.executionMode);

        List<Branch> branches = config.branches;
        if (branches.isEmpty()) {
            logger.warn("并行节点没有配置分支");
            return NodeResult.failed("并行节点没有配置分支");
        }

        List<String> selectedBranchIds;

        if ("ALL".equalsIgnoreCase(config.executionMode)) {
            // 模式1：执行所有分支
            selectedBranchIds = new ArrayList<>();
            for (Branch branch : branches) {
                selectedBranchIds.add(branch.id);
            }
        } else {
            // 模式2：Agent决策选择分支
            selectedBranchIds = selectBranchesViaAgent(node, branches, context);
        }

        if (selectedBranchIds.isEmpty()) {
            logger.warn("没有选择任何分支，使用默认分支");
            if (config.defaultBranches != null && !config.defaultBranches.isEmpty()) {
                selectedBranchIds = config.defaultBranches;
            } else {
                // 使用第一个分支作为默认
                selectedBranchIds.add(branches.get(0).id);
            }
        }

        // 构建目标节点ID列表
        List<String> targetNodeIds = new ArrayList<>();
        List<NodeResult.BranchInfo> branchInfos = new ArrayList<>();

        for (Branch branch : branches) {
            if (selectedBranchIds.contains(branch.id)) {
                targetNodeIds.add(branch.targetNodeId);
                branchInfos.add(new NodeResult.BranchInfo(
                        branch.id,
                        branch.targetNodeId,
                        branch.name,
                        branch.description
                ));
            }
        }

        logger.info("并行节点选择的分支: {}", selectedBranchIds);

        // 构建并行上下文
        NodeResult.ParallelContext parallelContext = new NodeResult.ParallelContext();
        parallelContext.setParallelNodeId(node.getId());
        parallelContext.setBranches(branchInfos);
        parallelContext.setMergeNodeId(config.mergeNodeId);

        NodeResult result = NodeResult.successWithNodes(
                "并行执行 " + targetNodeIds.size() + " 个分支",
                targetNodeIds
        );
        result.setParallelContext(parallelContext);

        return result;
    }

    /**
     * 通过Agent决策选择分支
     */
    private List<String> selectBranchesViaAgent(WorkflowNode node, List<Branch> branches,
                                                 NodeExecutionContext context) throws Exception {
        // 构建分支信息列表
        List<NodePromptBuilder.BranchInfo> branchInfos = new ArrayList<>();
        for (Branch branch : branches) {
            branchInfos.add(new NodePromptBuilder.BranchInfo(
                    branch.id,
                    branch.name,
                    branch.description,
                    branch.conditionDesc  // 条件描述（给Agent看）
            ));
        }

        // 使用通用的提示词构建器
        String prompt = NodePromptBuilder.buildParallelPrompt(
                context.getWorkflowId(),
                context.getExecutionId(),
                node.getId(),
                node.getName(),
                "DYNAMIC",  // 只有DYNAMIC模式才会调用这个方法
                branchInfos,
                context.getPreviousOutputs(),
                context.getTaskDescription(),
                parseConfig(node).customPrompt
        );

        // 调用Agent获取决策
        OpenClawGatewayClient client = new OpenClawGatewayClient(gatewayUrl, gatewayToken);
        String sessionContext = String.format("%s_%s_%s_parallel",
                context.getWorkflowId(),
                context.getExecutionId(),
                node.getId());

        OpenClawGatewayClient.AgentRequest request = OpenClawGatewayClient.AgentRequest.builder()
                .agentId(decisionAgentId)
                .message(prompt)
                .context(sessionContext)
                .build();

        logger.info("并行决策Agent调用: {} - 提示词长度: {}", node.getName(), prompt.length());

        OpenClawGatewayClient.AgentResponse response = client.executeAgent(request);

        if (!response.isSuccess()) {
            logger.error("决策Agent执行失败: {}", response.getErrorMessage());
            return getDefaultBranchIds(branches);
        }

        // 解析决策
        AgentDecisionParser.AgentDecision decision = AgentDecisionParser.parse(response.getContent());

        if (decision == null || decision.getNodeIds() == null || decision.getNodeIds().isEmpty()) {
            logger.warn("Agent未返回有效决策，使用默认分支");
            return getDefaultBranchIds(branches);
        }

        // 验证决策
        List<String> validBranchIds = new ArrayList<>();
        for (Branch branch : branches) {
            validBranchIds.add(branch.id);
        }

        List<String> validNodeIds = new ArrayList<>();
        for (String nodeId : decision.getNodeIds()) {
            if (validBranchIds.contains(nodeId)) {
                validNodeIds.add(nodeId);
            } else {
                logger.warn("Agent返回了无效的分支ID: {}", nodeId);
            }
        }

        return validNodeIds.isEmpty() ? getDefaultBranchIds(branches) : validNodeIds;
    }

    private List<String> getDefaultBranchIds(List<Branch> branches) {
        List<String> defaultIds = new ArrayList<>();
        for (Branch branch : branches) {
            defaultIds.add(branch.id);
        }
        return defaultIds;
    }

    /**
     * 解析节点配置
     */
    private ParallelConfig parseConfig(WorkflowNode node) {
        ParallelConfig config = new ParallelConfig();
        config.executionMode = "ALL"; // 默认执行所有分支

        try {
            if (node.getConfig() != null) {
                JsonNode jsonConfig = objectMapper.readTree(node.getConfig());

                // 解析执行模式
                if (jsonConfig.has("executionMode")) {
                    config.executionMode = jsonConfig.get("executionMode").asText();
                }

                // 解析决策Agent
                if (jsonConfig.has("decisionAgentId")) {
                    decisionAgentId = jsonConfig.get("decisionAgentId").asText();
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
                        // 新增：条件描述（给Agent看）
                        branch.conditionDesc = branchNode.has("conditionDesc") ? branchNode.get("conditionDesc").asText() : null;
                        config.branches.add(branch);
                    }
                }

                // 解析合并节点
                if (jsonConfig.has("mergeNode")) {
                    config.mergeNodeId = jsonConfig.get("mergeNode").asText();
                }

                // 解析默认分支
                if (jsonConfig.has("defaultBranches") && jsonConfig.get("defaultBranches").isArray()) {
                    config.defaultBranches = new ArrayList<>();
                    for (JsonNode defaultBranch : jsonConfig.get("defaultBranches")) {
                        config.defaultBranches.add(defaultBranch.asText());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("解析并行节点配置失败: {}", e.getMessage(), e);
        }

        return config;
    }

    // ==================== 内部类 ====================

    private static class ParallelConfig {
        String executionMode = "ALL";
        List<Branch> branches = new ArrayList<>();
        String mergeNodeId;
        List<String> defaultBranches;
        String customPrompt;  // 自定义提示词
    }

    private static class Branch {
        String id;
        String name;
        String description;
        String targetNodeId;
        String conditionDesc;  // 执行条件说明（给Agent看）
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