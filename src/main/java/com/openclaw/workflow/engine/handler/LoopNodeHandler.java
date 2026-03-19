package com.openclaw.workflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.connector.OpenClawGatewayClient;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.engine.util.AgentDecisionParser;
import com.openclaw.workflow.entity.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
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
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DEFAULT_MAX_ITERATIONS = 100;

    // Gateway API配置
    private String gatewayUrl = "http://localhost:18789";
    private String gatewayToken = "56b640cc2d91411f63255af68355c19ee33c88ec458878ca";

    // 决策Agent配置
    private String decisionAgentId = "project-manager";

    @Override
    public NodeResult execute(NodeExecutionContext context) throws Exception {
        WorkflowNode node = context.getNode();

        // 解析节点配置
        LoopConfig config = parseConfig(node);

        // 获取或初始化循环状态
        NodeResult.LoopContext loopContext = getOrCreateLoopContext(context, config);

        logger.info("执行循环节点: {} (迭代: {}/{})",
                node.getName(), loopContext.getCurrentIteration(), loopContext.getMaxIterations());

        // 检查是否达到最大迭代次数
        if (loopContext.getCurrentIteration() >= loopContext.getMaxIterations()) {
            logger.warn("达到最大迭代次数: {}", loopContext.getMaxIterations());
            return createExitResult(node, loopContext, "达到最大迭代次数");
        }

        // 检查迭代数据源
        if (config.loopMode == LoopMode.ITERATOR) {
            // 迭代模式：检查是否还有数据
            if (loopContext.getCurrentValue() == null) {
                logger.info("迭代数据已耗尽，退出循环");
                return createExitResult(node, loopContext, "迭代数据已耗尽");
            }

            // 进入下一次迭代
            loopContext.setCurrentIteration(loopContext.getCurrentIteration() + 1);
            return createContinueResult(config, loopContext);

        } else {
            // 条件模式：Agent决策是否继续
            return decideViaAgent(node, config, loopContext, context);
        }
    }

    /**
     * 获取或创建循环上下文
     */
    private NodeResult.LoopContext getOrCreateLoopContext(NodeExecutionContext context, LoopConfig config) {
        // 尝试从元数据中获取现有上下文
        if (context.getPreviousOutputs() != null) {
            for (NodeResult result : context.getPreviousOutputs().values()) {
                if (result != null && result.getLoopContext() != null) {
                    NodeResult.LoopContext existing = result.getLoopContext();
                    // 更新当前值（迭代模式）
                    if (config.loopMode == LoopMode.ITERATOR && config.iteratorSource != null) {
                        int nextIndex = existing.getCurrentIteration();
                        if (nextIndex < config.iteratorSource.size()) {
                            existing.setCurrentValue(config.iteratorSource.get(nextIndex));
                        } else {
                            existing.setCurrentValue(null);
                        }
                    }
                    return existing;
                }
            }
        }

        // 创建新的循环上下文
        NodeResult.LoopContext loopContext = new NodeResult.LoopContext();
        loopContext.setCurrentIteration(0);
        loopContext.setMaxIterations(config.maxIterations);
        loopContext.setLoopVariable(config.loopVariable);
        loopContext.setIterationResults(new ArrayList<>());

        // 设置初始值（迭代模式）
        if (config.loopMode == LoopMode.ITERATOR && config.iteratorSource != null
                && !config.iteratorSource.isEmpty()) {
            loopContext.setCurrentValue(config.iteratorSource.get(0));
        }

        return loopContext;
    }

    /**
     * 通过Agent决策是否继续循环
     */
    private NodeResult decideViaAgent(WorkflowNode node, LoopConfig config,
                                       NodeResult.LoopContext loopContext,
                                       NodeExecutionContext context) throws Exception {
        // 构建决策提示
        StringBuilder prompt = new StringBuilder();
        prompt.append("## 循环执行决策\n\n");
        prompt.append("当前节点：").append(node.getName()).append("\n");
        prompt.append("当前迭代：第 ").append(loopContext.getCurrentIteration() + 1).append(" 次\n");
        prompt.append("最大迭代：").append(loopContext.getMaxIterations()).append(" 次\n\n");

        // 添加迭代结果历史
        if (loopContext.getIterationResults() != null && !loopContext.getIterationResults().isEmpty()) {
            prompt.append("### 已完成的迭代：\n");
            for (int i = 0; i < loopContext.getIterationResults().size(); i++) {
                prompt.append("- 迭代 ").append(i + 1).append(": ");
                prompt.append(loopContext.getIterationResults().get(i)).append("\n");
            }
            prompt.append("\n");
        }

        // 添加任务背景
        if (context.getTaskDescription() != null) {
            prompt.append("### 任务背景\n").append(context.getTaskDescription()).append("\n\n");
        }

        // 添加决策选项
        prompt.append("### 请决定下一步操作：\n\n");
        prompt.append("- `continue`: 继续下一次迭代\n");
        prompt.append("- `exit`: 退出循环（任务已完成或无法继续）\n\n");

        prompt.append("### 输出格式：\n");
        prompt.append("```\n");
        prompt.append("[NODE_DECISION]\n");
        prompt.append("node_ids: continue 或 exit\n");
        prompt.append("reason: <决策原因>\n");
        prompt.append("[/NODE_DECISION]\n");
        prompt.append("```\n");

        // 调用Agent获取决策
        OpenClawGatewayClient client = new OpenClawGatewayClient(gatewayUrl, gatewayToken);
        String sessionContext = String.format("%s_%s_%s_loop_%d",
                context.getWorkflowId(),
                context.getExecutionId(),
                node.getId(),
                loopContext.getCurrentIteration());

        OpenClawGatewayClient.AgentRequest request = OpenClawGatewayClient.AgentRequest.builder()
                .agentId(decisionAgentId)
                .message(prompt.toString())
                .context(sessionContext)
                .build();

        OpenClawGatewayClient.AgentResponse response = client.executeAgent(request);

        // 解析决策
        String decision = "exit"; // 默认退出

        if (response.isSuccess()) {
            AgentDecisionParser.AgentDecision agentDecision = AgentDecisionParser.parse(response.getContent());
            if (agentDecision != null && !agentDecision.getNodeIds().isEmpty()) {
                String firstNodeId = agentDecision.getFirstNodeId();
                if ("continue".equalsIgnoreCase(firstNodeId) || "exit".equalsIgnoreCase(firstNodeId)) {
                    decision = firstNodeId.toLowerCase();
                }
            }
        }

        // 执行决策
        if ("continue".equals(decision)) {
            loopContext.setCurrentIteration(loopContext.getCurrentIteration() + 1);
            return createContinueResult(config, loopContext);
        } else {
            return createExitResult(node, loopContext, "Agent决定退出循环");
        }
    }

    /**
     * 创建继续迭代的结果
     */
    private NodeResult createContinueResult(LoopConfig config, NodeResult.LoopContext loopContext) {
        loopContext.setShouldContinue(true);

        NodeResult result = NodeResult.success(
                String.format("迭代 %d/%d", loopContext.getCurrentIteration(), loopContext.getMaxIterations()),
                config.loopBodyEntryNode
        );
        result.setLoopContext(loopContext);

        // 将循环变量添加到元数据
        result.addMetadata("loopVariable", loopContext.getLoopVariable());
        result.addMetadata("currentValue", loopContext.getCurrentValue());
        result.addMetadata("iteration", loopContext.getCurrentIteration());

        return result;
    }

    /**
     * 创建退出循环的结果
     */
    private NodeResult createExitResult(WorkflowNode node, NodeResult.LoopContext loopContext, String reason) {
        loopContext.setShouldContinue(false);

        // 获取循环完成后的目标节点
        String nextNodeId = parseOnLoopComplete(node);

        NodeResult result = NodeResult.success(
                String.format("循环完成，共迭代 %d 次。原因: %s", loopContext.getCurrentIteration(), reason),
                nextNodeId
        );
        result.setLoopContext(loopContext);
        result.setDecisionReason(reason);

        return result;
    }

    /**
     * 解析节点配置
     */
    private LoopConfig parseConfig(WorkflowNode node) {
        LoopConfig config = new LoopConfig();
        config.maxIterations = DEFAULT_MAX_ITERATIONS;
        config.loopMode = LoopMode.CONDITION; // 默认条件模式

        try {
            if (node.getConfig() != null) {
                JsonNode jsonConfig = objectMapper.readTree(node.getConfig());

                // 解析循环模式
                if (jsonConfig.has("loopMode")) {
                    String mode = jsonConfig.get("loopMode").asText();
                    config.loopMode = "iterator".equalsIgnoreCase(mode) ? LoopMode.ITERATOR : LoopMode.CONDITION;
                }

                // 解析循环配置
                if (jsonConfig.has("loopConfig")) {
                    JsonNode loopConfig = jsonConfig.get("loopConfig");

                    if (loopConfig.has("maxIterations")) {
                        config.maxIterations = loopConfig.get("maxIterations").asInt();
                    }

                    if (loopConfig.has("loopVariable")) {
                        config.loopVariable = loopConfig.get("loopVariable").asText();
                    }

                    if (loopConfig.has("iteratorSource") && loopConfig.get("iteratorSource").isArray()) {
                        config.iteratorSource = new ArrayList<>();
                        for (JsonNode item : loopConfig.get("iteratorSource")) {
                            config.iteratorSource.add(item);
                        }
                    }
                }

                // 解析循环体配置
                if (jsonConfig.has("loopBody")) {
                    JsonNode loopBody = jsonConfig.get("loopBody");
                    if (loopBody.has("entryNodeId")) {
                        config.loopBodyEntryNode = loopBody.get("entryNodeId").asText();
                    }
                    if (loopBody.has("exitNodeId")) {
                        config.loopBodyExitNode = loopBody.get("exitNodeId").asText();
                    }
                }

                // 解析决策Agent
                if (jsonConfig.has("decisionAgentId")) {
                    decisionAgentId = jsonConfig.get("decisionAgentId").asText();
                }
            }
        } catch (Exception e) {
            logger.error("解析循环节点配置失败: {}", e.getMessage(), e);
        }

        return config;
    }

    /**
     * 解析循环完成后的目标节点
     */
    private String parseOnLoopComplete(WorkflowNode node) {
        try {
            if (node.getConfig() != null) {
                JsonNode jsonConfig = objectMapper.readTree(node.getConfig());
                if (jsonConfig.has("onLoopComplete")) {
                    return jsonConfig.get("onLoopComplete").asText();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    // ==================== 内部类和枚举 ====================

    private enum LoopMode {
        ITERATOR,   // 迭代模式：遍历数据列表
        CONDITION   // 条件模式：Agent决策是否继续
    }

    private static class LoopConfig {
        LoopMode loopMode = LoopMode.CONDITION;
        int maxIterations = DEFAULT_MAX_ITERATIONS;
        String loopVariable = "item";
        List<Object> iteratorSource;
        String loopBodyEntryNode;
        String loopBodyExitNode;
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