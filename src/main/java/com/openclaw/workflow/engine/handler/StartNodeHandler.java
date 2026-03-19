package com.openclaw.workflow.engine.handler;

import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 开始节点处理器
 */
public class StartNodeHandler extends BaseNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(StartNodeHandler.class);

    @Override
    public NodeResult execute(NodeExecutionContext context) throws Exception {
        logger.info("工作流开始执行: {}", context.getExecutionId());
        return NodeResult.success("started");
    }
}