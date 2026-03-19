package com.openclaw.workflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.entity.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 等待节点处理器
 */
public class WaitNodeHandler extends BaseNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(WaitNodeHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public NodeResult execute(NodeExecutionContext context) throws Exception {
        WorkflowNode node = context.getNode();
        int duration = parseDuration(node);

        logger.info("执行等待: {}秒", duration);

        if (duration > 0) {
            TimeUnit.SECONDS.sleep(duration);
        }

        return NodeResult.success("waited " + duration + " seconds");
    }

    private int parseDuration(WorkflowNode node) {
        try {
            if (node.getConfig() != null) {
                JsonNode jsonConfig = objectMapper.readTree(node.getConfig());
                if (jsonConfig.has("duration")) {
                    return jsonConfig.get("duration").asInt();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return 0;
    }
}