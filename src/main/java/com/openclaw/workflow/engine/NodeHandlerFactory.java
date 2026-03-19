package com.openclaw.workflow.engine;

import com.openclaw.workflow.engine.handler.*;
import com.openclaw.workflow.entity.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 节点处理器工厂
 *
 * 负责根据节点类型创建对应的处理器实例
 */
@Component
public class NodeHandlerFactory {

    private static final Logger logger = LoggerFactory.getLogger(NodeHandlerFactory.class);

    // 单例处理器（有状态的处理器需要每次创建新实例）
    private final AgentNodeHandler agentNodeHandler;
    private final ParallelNodeHandler parallelNodeHandler;
    private final LoopNodeHandler loopNodeHandler;
    private final ConditionNodeHandler conditionNodeHandler;
    private final HumanReviewNodeHandler humanReviewNodeHandler;

    // Gateway API配置
    private String gatewayUrl = "http://localhost:18789";
    private String gatewayToken = "56b640cc2d91411f63255af68355c19ee33c88ec458878ca";

    public NodeHandlerFactory() {
        this.agentNodeHandler = new AgentNodeHandler();
        this.parallelNodeHandler = new ParallelNodeHandler();
        this.loopNodeHandler = new LoopNodeHandler();
        this.conditionNodeHandler = new ConditionNodeHandler();
        this.humanReviewNodeHandler = new HumanReviewNodeHandler();

        // 配置Gateway API
        configureHandlers();
    }

    private void configureHandlers() {
        // Agent处理器配置
        agentNodeHandler.setUseGatewayApi(true);
        agentNodeHandler.setGatewayUrl(gatewayUrl);
        agentNodeHandler.setGatewayToken(gatewayToken);

        // 并行处理器配置
        parallelNodeHandler.setGatewayUrl(gatewayUrl);
        parallelNodeHandler.setGatewayToken(gatewayToken);

        // 循环处理器配置
        loopNodeHandler.setGatewayUrl(gatewayUrl);
        loopNodeHandler.setGatewayToken(gatewayToken);

        // 条件处理器配置
        conditionNodeHandler.setGatewayUrl(gatewayUrl);
        conditionNodeHandler.setGatewayToken(gatewayToken);
    }

    /**
     * 根据节点类型获取处理器
     */
    public BaseNodeHandler getHandler(WorkflowNode.NodeType nodeType) {
        switch (nodeType) {
            case START:
                return new StartNodeHandler();
            case FINISH:
                return new FinishNodeHandler();
            case AGENT_EXECUTION:
                return agentNodeHandler;
            case API_CALL:
                return new ApiNodeHandler();
            case CONDITION:
                return conditionNodeHandler;
            case PARALLEL:
                return parallelNodeHandler;
            case LOOP:
                return loopNodeHandler;
            case WAIT:
                return new WaitNodeHandler();
            case HUMAN_REVIEW:
                return humanReviewNodeHandler;
            case SUBWORKFLOW:
                // 子工作流处理器（暂未实现）
                logger.warn("SUBWORKFLOW节点类型尚未完全实现");
                return new BaseNodeHandler() {
                    @Override
                    public com.openclaw.workflow.engine.model.NodeResult execute(
                            com.openclaw.workflow.engine.model.NodeExecutionContext context) {
                        return com.openclaw.workflow.engine.model.NodeResult.failed("子工作流节点尚未实现");
                    }
                };
            default:
                logger.warn("未知的节点类型: {}, 使用Agent处理器", nodeType);
                return agentNodeHandler;
        }
    }

    // ==================== 配置方法 ====================

    public void setOpenclawCommand(String command) {
        this.agentNodeHandler.setOpenclawCommand(command);
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
        configureHandlers();
    }

    public void setGatewayToken(String gatewayToken) {
        this.gatewayToken = gatewayToken;
        configureHandlers();
    }

    public void setFeishuWebhookUrl(String feishuWebhookUrl) {
        this.humanReviewNodeHandler.setFeishuWebhookUrl(feishuWebhookUrl);
    }

    public void setReviewServerUrl(String reviewServerUrl) {
        this.humanReviewNodeHandler.setReviewServerUrl(reviewServerUrl);
    }
}