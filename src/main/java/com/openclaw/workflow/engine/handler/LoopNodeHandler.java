package com.openclaw.workflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.engine.service.NodePromptService;
import com.openclaw.workflow.entity.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 循环执行节点处理器
 *
 * 支持两种循环模式：
 * 1. 迭代模式：遍历数据列表
 * 2. 条件模式：Agent决策是否继续
 *
 * Agent决策协议：
 * [NODE_DECISION]
 * node_ids: continue 或 exit
 * reason: 选择原因
 * [/NODE_DECISION]
 */
public class LoopNodeHandler extends BaseNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(LoopNodeHandler.class);
    private static final int DEFAULT_MAX_ITERATIONS = 100;

    private String gatewayUrl = "http://localhost:18789";
    private String gatewayToken = "56b640cc2d91411f63255af68355c19ee33c88ec458878ca";
    private String decisionAgentId = "project-manager";
    private NodePromptService promptService;

    @Override
    public NodeResult execute(NodeExecutionContext context) throws Exception {
        WorkflowNode node = context.getNode();
        LoopConfig config = parseConfig(node);
        NodeResult.LoopContext loopContext = getOrCreateLoopContext(context, config);

        logger.info("执行循环节点: {} (迭代: {}/{})",
                node.getName(), loopContext.getCurrentIteration(), loopContext.getMaxIterations());

        if (loopContext.getCurrentIteration() >= loopContext.getMaxIterations()) {
            logger.warn("达到最大迭代次数: {}", loopContext.getMaxIterations());
            return createExitResult(node, loopContext, "达到最大迭代次数");
        }

        if (config.getLoopMode() == LoopMode.ITERATOR) {
            if (loopContext.getCurrentValue() == null) {
                logger.info("迭代数据已耗尽，退出循环");
                return createExitResult(node, loopContext, "迭代数据已耗尽");
            }
            loopContext.setCurrentIteration(loopContext.getCurrentIteration() + 1);
            return createContinueResult(config, loopContext);
        } else {
            return decideViaAgent(node, config, loopContext, context);
        }
    }

    private NodeResult.LoopContext getOrCreateLoopContext(NodeExecutionContext context, LoopConfig config) {
        if (context.getPreviousOutputs() != null) {
            for (NodeResult result : context.getPreviousOutputs().values()) {
                if (result != null && result.getLoopContext() != null) {
                    NodeResult.LoopContext existing = result.getLoopContext();
                    if (config.getLoopMode() == LoopMode.ITERATOR && config.getIteratorSource() != null) {
                        int nextIndex = existing.getCurrentIteration();
                        if (nextIndex < config.getIteratorSource().size()) {
                            existing.setCurrentValue(config.getIteratorSource().get(nextIndex));
                        } else {
                            existing.setCurrentValue(null);
                        }
                    }
                    return existing;
                }
            }
        }

        NodeResult.LoopContext loopContext = new NodeResult.LoopContext();
        loopContext.setCurrentIteration(0);
        loopContext.setMaxIterations(config.getMaxIterations());
        loopContext.setLoopVariable(config.getLoopVariable());
        loopContext.setIterationResults(new ArrayList<>());

        if (config.getLoopMode() == LoopMode.ITERATOR && config.getIteratorSource() != null
                && !config.getIteratorSource().isEmpty()) {
            loopContext.setCurrentValue(config.getIteratorSource().get(0));
        }

        return loopContext;
    }

    private NodeResult decideViaAgent(WorkflowNode node, LoopConfig config,
                                       NodeResult.LoopContext loopContext,
                                       NodeExecutionContext context) {
        LoopDecisionHelper helper = new LoopDecisionHelper(
                gatewayUrl, gatewayToken, decisionAgentId, promptService);
        String decision = helper.decide(node, config, loopContext, context);

        if ("continue".equals(decision)) {
            loopContext.setCurrentIteration(loopContext.getCurrentIteration() + 1);
            return createContinueResult(config, loopContext);
        } else {
            return createExitResult(node, loopContext, "Agent决定退出循环");
        }
    }

    private NodeResult createContinueResult(LoopConfig config, NodeResult.LoopContext loopContext) {
        loopContext.setShouldContinue(true);

        NodeResult result = NodeResult.success(
                String.format("迭代 %d/%d", loopContext.getCurrentIteration(), loopContext.getMaxIterations()),
                config.getLoopBodyEntryNode()
        );
        result.setLoopContext(loopContext);
        result.addMetadata("loopVariable", loopContext.getLoopVariable());
        result.addMetadata("currentValue", loopContext.getCurrentValue());
        result.addMetadata("iteration", loopContext.getCurrentIteration());

        return result;
    }

    private NodeResult createExitResult(WorkflowNode node, NodeResult.LoopContext loopContext, String reason) {
        loopContext.setShouldContinue(false);

        String nextNodeId = parseOnLoopComplete(node);
        NodeResult result = NodeResult.success(
                String.format("循环完成，共迭代 %d 次。原因: %s", loopContext.getCurrentIteration(), reason),
                nextNodeId
        );
        result.setLoopContext(loopContext);
        result.setDecisionReason(reason);

        return result;
    }

    private LoopConfig parseConfig(WorkflowNode node) {
        LoopConfig config = new LoopConfig();
        JsonNode jsonConfig = NodeConfigParser.parseJson(node.getConfig());
        if (jsonConfig == null) {
            return config;
        }

        String mode = NodeConfigParser.getString(jsonConfig, "loopMode", "condition");
        config.setLoopMode("iterator".equalsIgnoreCase(mode) ? LoopMode.ITERATOR : LoopMode.CONDITION);
        config.setCustomPrompt(NodeConfigParser.getString(jsonConfig, "customPrompt", null));

        JsonNode loopConfigNode = NodeConfigParser.getNode(jsonConfig, "loopConfig");
        if (loopConfigNode != null) {
            config.setMaxIterations(NodeConfigParser.getInt(loopConfigNode, "maxIterations", DEFAULT_MAX_ITERATIONS));
            config.setLoopVariable(NodeConfigParser.getString(loopConfigNode, "loopVariable", "item"));
            config.setExitCondition(NodeConfigParser.getString(loopConfigNode, "exitCondition", null));

            JsonNode iteratorSourceNode = NodeConfigParser.getNode(loopConfigNode, "iteratorSource");
            if (iteratorSourceNode != null && iteratorSourceNode.isArray()) {
                List<Object> iteratorSource = new ArrayList<>();
                for (JsonNode item : iteratorSourceNode) {
                    iteratorSource.add(item);
                }
                config.setIteratorSource(iteratorSource);
            }
        }

        JsonNode loopBodyNode = NodeConfigParser.getNode(jsonConfig, "loopBody");
        if (loopBodyNode != null) {
            config.setLoopBodyEntryNode(NodeConfigParser.getString(loopBodyNode, "entryNodeId", null));
            config.setLoopBodyExitNode(NodeConfigParser.getString(loopBodyNode, "exitNodeId", null));
        }

        String agentId = NodeConfigParser.getString(jsonConfig, "decisionAgentId", null);
        if (agentId != null) {
            this.decisionAgentId = agentId;
        }

        return config;
    }

    private String parseOnLoopComplete(WorkflowNode node) {
        JsonNode jsonConfig = NodeConfigParser.parseJson(node.getConfig());
        return NodeConfigParser.getString(jsonConfig, "onLoopComplete", null);
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