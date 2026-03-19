package com.openclaw.workflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.entity.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 人工审核节点处理器
 */
public class HumanReviewNodeHandler extends BaseNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(HumanReviewNodeHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DEFAULT_TIMEOUT = 3600;

    @Override
    public NodeResult execute(NodeExecutionContext context) throws Exception {
        WorkflowNode node = context.getNode();
        int timeout = parseTimeout(node);

        logger.info("等待人工审核: 节点={}, 超时={}秒", node.getName(), timeout);

        // 返回retry状态，让外部系统处理审核逻辑
        return NodeResult.retry();
    }

    private int parseTimeout(WorkflowNode node) {
        try {
            if (node.getConfig() != null) {
                JsonNode jsonConfig = objectMapper.readTree(node.getConfig());
                if (jsonConfig.has("timeout")) {
                    return jsonConfig.get("timeout").asInt();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return DEFAULT_TIMEOUT;
    }
}