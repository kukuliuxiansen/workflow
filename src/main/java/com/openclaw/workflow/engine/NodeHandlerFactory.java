package com.openclaw.workflow.engine;

import com.openclaw.workflow.engine.handler.*;
import com.openclaw.workflow.entity.WorkflowNode;
import org.springframework.stereotype.Component;

/**
 * 节点处理器工厂
 */
@Component
public class NodeHandlerFactory {

    private final AgentNodeHandler agentNodeHandler = new AgentNodeHandler();

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
                return new ConditionNodeHandler();
            case WAIT:
                return new WaitNodeHandler();
            case HUMAN_REVIEW:
                return new HumanReviewNodeHandler();
            case PARALLEL:
            case LOOP:
            case SUBWORKFLOW:
                return new BaseNodeHandler() {
                    @Override
                    public com.openclaw.workflow.engine.model.NodeResult execute(
                            com.openclaw.workflow.engine.model.NodeExecutionContext context) {
                        return com.openclaw.workflow.engine.model.NodeResult.success("not implemented");
                    }
                };
            default:
                return agentNodeHandler;
        }
    }

    public void setOpenclawCommand(String command) {
        this.agentNodeHandler.setOpenclawCommand(command);
    }
}