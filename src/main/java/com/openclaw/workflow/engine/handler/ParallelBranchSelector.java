package com.openclaw.workflow.engine.handler;

import com.openclaw.workflow.engine.connector.AgentRequest;
import com.openclaw.workflow.engine.connector.AgentResponse;
import com.openclaw.workflow.engine.connector.OpenClawGatewayClient;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.service.BranchInfo;
import com.openclaw.workflow.engine.service.NodePromptService;
import com.openclaw.workflow.engine.util.AgentDecisionParser;
import com.openclaw.workflow.engine.util.NodePromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 并行分支选择器
 * 负责通过Agent决策选择要执行的分支
 */
public class ParallelBranchSelector {

    private static final Logger logger = LoggerFactory.getLogger(ParallelBranchSelector.class);

    private String gatewayUrl = "http://localhost:18789";
    private String gatewayToken = "56b640cc2d91411f63255af68355c19ee33c88ec458878ca";
    private String decisionAgentId = "project-manager";
    private NodePromptService promptService;

    public List<String> selectBranches(ParallelConfig config, NodeExecutionContext context) throws Exception {
        String prompt = buildPrompt(config, context);
        AgentResponse response = executeAgent(config, context, prompt);

        if (!response.isSuccess()) {
            logger.error("决策Agent执行失败: {}", response.getErrorMessage());
            return getAllBranchIds(config.getBranches());
        }

        return parseAndValidateDecision(response.getContent(), config.getBranches());
    }

    private String buildPrompt(ParallelConfig config, NodeExecutionContext context) {
        List<Branch> branches = config.getBranches();

        if (promptService != null) {
            List<BranchInfo> branchInfos = convertToPromptServiceBranches(branches);
            return promptService.buildParallelPrompt(
                    context.getWorkflowId(),
                    context.getExecutionId(),
                    context.getNode().getId(),
                    context.getNode().getName(),
                    "DYNAMIC",
                    branchInfos,
                    context.getPreviousOutputs(),
                    context.getTaskDescription(),
                    config.getCustomPrompt()
            );
        }

        List<NodePromptBuilder.BranchInfo> branchInfos = convertToPromptBuilderBranches(branches);
        return NodePromptBuilder.buildParallelPrompt(
                context.getWorkflowId(),
                context.getExecutionId(),
                context.getNode().getId(),
                context.getNode().getName(),
                "DYNAMIC",
                branchInfos,
                context.getPreviousOutputs(),
                context.getTaskDescription(),
                config.getCustomPrompt()
        );
    }

    private List<BranchInfo> convertToPromptServiceBranches(List<Branch> branches) {
        List<BranchInfo> result = new ArrayList<>();
        for (Branch branch : branches) {
            result.add(new BranchInfo(
                    branch.getId(),
                    branch.getName(),
                    branch.getDescription(),
                    branch.getConditionDesc()
            ));
        }
        return result;
    }

    private List<NodePromptBuilder.BranchInfo> convertToPromptBuilderBranches(List<Branch> branches) {
        List<NodePromptBuilder.BranchInfo> result = new ArrayList<>();
        for (Branch branch : branches) {
            result.add(new NodePromptBuilder.BranchInfo(
                    branch.getId(),
                    branch.getName(),
                    branch.getDescription(),
                    branch.getConditionDesc()
            ));
        }
        return result;
    }

    private AgentResponse executeAgent(ParallelConfig config,
                                        NodeExecutionContext context,
                                        String prompt) throws Exception {
        OpenClawGatewayClient client = new OpenClawGatewayClient(gatewayUrl, gatewayToken);
        String sessionContext = String.format("%s_%s_%s_parallel",
                context.getWorkflowId(),
                context.getExecutionId(),
                context.getNode().getId());

        AgentRequest request = AgentRequest.builder()
                .agentId(decisionAgentId)
                .message(prompt)
                .context(sessionContext)
                .build();

        logger.info("并行决策Agent调用: {} - 提示词长度: {}",
                context.getNode().getName(), prompt.length());

        return client.executeAgent(request);
    }

    private List<String> parseAndValidateDecision(String content, List<Branch> branches) {
        AgentDecisionParser.AgentDecision decision = AgentDecisionParser.parse(content);

        if (decision == null || decision.getNodeIds() == null || decision.getNodeIds().isEmpty()) {
            logger.warn("Agent未返回有效决策，使用默认分支");
            return getAllBranchIds(branches);
        }

        List<String> validBranchIds = getAllBranchIds(branches);
        List<String> validNodeIds = new ArrayList<>();

        for (String nodeId : decision.getNodeIds()) {
            if (validBranchIds.contains(nodeId)) {
                validNodeIds.add(nodeId);
            } else {
                logger.warn("Agent返回了无效的分支ID: {}", nodeId);
            }
        }

        return validNodeIds.isEmpty() ? getAllBranchIds(branches) : validNodeIds;
    }

    private List<String> getAllBranchIds(List<Branch> branches) {
        List<String> ids = new ArrayList<>();
        for (Branch branch : branches) {
            ids.add(branch.getId());
        }
        return ids;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public void setGatewayToken(String gatewayToken) {
        this.gatewayToken = gatewayToken;
    }

    public void setDecisionAgentId(String decisionAgentId) {
        this.decisionAgentId = decisionAgentId;
    }

    public void setPromptService(NodePromptService promptService) {
        this.promptService = promptService;
    }
}