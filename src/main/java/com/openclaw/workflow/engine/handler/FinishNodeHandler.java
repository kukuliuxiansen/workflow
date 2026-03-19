package com.openclaw.workflow.engine.handler;

import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 结束节点处理器
 */
public class FinishNodeHandler extends BaseNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(FinishNodeHandler.class);

    @Override
    public NodeResult execute(NodeExecutionContext context) throws Exception {
        logger.info("工作流执行完成: {}", context.getExecutionId());
        return NodeResult.success("completed");
    }
}