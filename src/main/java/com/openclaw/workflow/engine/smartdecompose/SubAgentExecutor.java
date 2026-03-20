package com.openclaw.workflow.engine.smartdecompose;

import com.openclaw.workflow.engine.connector.OpenClawGatewayClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 子Agent执行器
 * 负责启动和管理子Agent的执行
 */
public class SubAgentExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SubAgentExecutor.class);

    private final String gatewayUrl;
    private final String gatewayToken;
    private final SubAgentPromptBuilder promptBuilder;
    private final SubAgentResultParser resultParser;

    public SubAgentExecutor(String gatewayUrl, String gatewayToken) {
        this.gatewayUrl = gatewayUrl;
        this.gatewayToken = gatewayToken;
        this.promptBuilder = new SubAgentPromptBuilder();
        this.resultParser = new SubAgentResultParser();
    }

    /**
     * 执行子Agent
     */
    public SubAgentResult execute(SubAgentRequest request) {
        logger.info(">>> 启动子Agent: type={}, prompt={}",
            request.getAgentType().getName(),
            truncate(request.getPrompt(), 100));

        long startTime = System.currentTimeMillis();

        try {
            String systemPrompt = promptBuilder.buildSystemPrompt(request.getAgentType());
            String userPrompt = promptBuilder.buildUserPrompt(request);

            OpenClawGatewayClient client = new OpenClawGatewayClient(gatewayUrl, gatewayToken);

            String agentId = promptBuilder.getAgentId(request.getAgentType());
            String contextKey = "subagent_" + request.getParentExecutionId() + "_" + System.currentTimeMillis();

            logger.info("子Agent API请求: agentId={}, context={}", agentId, contextKey);

            OpenClawGatewayClient.AgentRequest agentRequest = OpenClawGatewayClient.AgentRequest.builder()
                .agentId(agentId)
                .systemPrompt(systemPrompt)
                .message(userPrompt)
                .context(contextKey)
                .maxTokens(2048)
                .temperature(0.7)
                .build();

            OpenClawGatewayClient.AgentResponse response = client.executeAgent(agentRequest);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("子Agent API响应: success={}, duration={}ms, tokens={}",
                response.isSuccess(), duration, response.getTotalTokens());

            if (!response.isSuccess()) {
                return SubAgentResult.error("子Agent执行失败: " + response.getErrorMessage());
            }

            String content = response.getContent();
            logger.info("子Agent输出 (前500字符):\n{}", truncate(content, 500));

            SubAgentResult result = resultParser.parse(content);
            result.setTotalTokens(response.getTotalTokens());
            result.setDurationMs(duration);

            logger.info("子Agent执行完成: success={}, summary={}",
                result.isSuccess(), truncate(result.getSummary(), 100));

            return result;

        } catch (Exception e) {
            logger.error("子Agent执行异常: {}", e.getMessage(), e);
            return SubAgentResult.error("执行异常: " + e.getMessage());
        }
    }

    /**
     * 并行执行多个子Agent
     */
    public List<SubAgentResult> executeParallel(List<SubAgentRequest> requests) {
        logger.info("并行执行 {} 个子Agent", requests.size());

        List<SubAgentResult> results = new ArrayList<>();

        // 目前简化为顺序执行，后续可以改为真正的并行
        for (SubAgentRequest request : requests) {
            results.add(execute(request));
        }

        return results;
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}