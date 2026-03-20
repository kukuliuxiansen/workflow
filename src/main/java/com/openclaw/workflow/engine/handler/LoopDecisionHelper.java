package com.openclaw.workflow.engine.handler;

import com.openclaw.workflow.engine.connector.AgentRequest;
import com.openclaw.workflow.engine.connector.AgentResponse;
import com.openclaw.workflow.engine.connector.OpenClawGatewayClient;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.engine.service.NodePromptService;
import com.openclaw.workflow.engine.util.AgentDecisionParser;
import com.openclaw.workflow.engine.util.NodePromptBuilder;
import com.openclaw.workflow.entity.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 循环节点Agent决策辅助类
 */
public class LoopDecisionHelper {

    private static final Logger logger = LoggerFactory.getLogger(LoopDecisionHelper.class);

    private final String gatewayUrl;
    private final String gatewayToken;
    private final String decisionAgentId;
    private final NodePromptService promptService;

    public LoopDecisionHelper(String gatewayUrl, String gatewayToken,
                              String decisionAgentId, NodePromptService promptService) {
        this.gatewayUrl = gatewayUrl;
        this.gatewayToken = gatewayToken;
        this.decisionAgentId = decisionAgentId;
        this.promptService = promptService;
    }

    /**
     * 通过Agent决策是否继续循环
     *
     * @return "continue" 或 "exit"
     */
    public String decide(WorkflowNode node, LoopConfig config,
                         NodeResult.LoopContext loopContext,
                         NodeExecutionContext context) {
        try {
            String prompt = buildPrompt(context, node, config, loopContext);

            OpenClawGatewayClient client = new OpenClawGatewayClient(gatewayUrl, gatewayToken);
            String sessionContext = String.format("%s_%s_%s_loop_%d",
                    context.getWorkflowId(), context.getExecutionId(), node.getId(),
                    loopContext.getCurrentIteration());

            AgentRequest request = AgentRequest.builder()
                    .agentId(decisionAgentId)
                    .message(prompt)
                    .context(sessionContext)
                    .build();

            logger.info("循环决策Agent调用: {} (迭代{}) - 提示词长度: {}",
                    node.getName(), loopContext.getCurrentIteration() + 1, prompt.length());

            AgentResponse response = client.executeAgent(request);
            return parseAgentDecision(response);
        } catch (Exception e) {
            logger.error("循环决策Agent调用失败: {}", e.getMessage());
            return "exit";
        }
    }

    private String buildPrompt(NodeExecutionContext context, WorkflowNode node,
                               LoopConfig config, NodeResult.LoopContext loopContext) {
        if (promptService != null) {
            return promptService.buildLoopPrompt(
                    context.getWorkflowId(), context.getExecutionId(), node.getId(), node.getName(),
                    "condition", loopContext.getCurrentIteration() + 1, loopContext.getMaxIterations(),
                    config.getExitCondition(), config.getLoopVariable(), loopContext.getCurrentValue(),
                    context.getPreviousOutputs(), config.getCustomPrompt()
            );
        }
        return NodePromptBuilder.buildLoopPrompt(
                context.getWorkflowId(), context.getExecutionId(), node.getId(), node.getName(),
                "condition", loopContext.getCurrentIteration() + 1, loopContext.getMaxIterations(),
                config.getExitCondition(), config.getLoopVariable(), loopContext.getCurrentValue(),
                context.getPreviousOutputs(), config.getCustomPrompt()
        );
    }

    private String parseAgentDecision(AgentResponse response) {
        if (!response.isSuccess()) {
            logger.error("循环决策Agent执行失败: {}", response.getErrorMessage());
            return "exit";
        }

        AgentDecisionParser.AgentDecision agentDecision = AgentDecisionParser.parse(response.getContent());
        if (agentDecision != null && !agentDecision.getNodeIds().isEmpty()) {
            String firstNodeId = agentDecision.getFirstNodeId();
            if ("continue".equalsIgnoreCase(firstNodeId) || "exit".equalsIgnoreCase(firstNodeId)) {
                return firstNodeId.toLowerCase();
            }
        }
        return "exit";
    }
}