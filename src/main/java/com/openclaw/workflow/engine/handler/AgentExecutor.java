package com.openclaw.workflow.engine.handler;

import com.openclaw.workflow.engine.connector.OpenClawGatewayClient;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Agent执行器
 *
 * 支持两种执行模式：
 * 1. Gateway API模式（推荐）：通过HTTP调用Agent，支持会话隔离
 * 2. CLI模式：通过命令行调用Agent（向后兼容）
 */
public class AgentExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AgentExecutor.class);
    private static final int DEFAULT_TIMEOUT = 600;

    // CLI模式配置
    private String openclawCommand = "openclaw";

    // Gateway API配置
    private boolean useGatewayApi = true;
    private String gatewayUrl = "http://localhost:18789";
    private String gatewayToken = "56b640cc2d91411f63255af68355c19ee33c88ec458878ca";

    /**
     * 执行Agent
     *
     * @param agentId Agent ID
     * @param prompt 提示词
     * @param context 执行上下文
     * @param timeout 超时时间（秒）
     * @return Agent输出
     */
    public String execute(String agentId, String prompt, NodeExecutionContext context, int timeout) throws Exception {
        logger.info("执行Agent: {} - 节点: {} (模式: {})",
                agentId, context.getNode().getName(), useGatewayApi ? "Gateway API" : "CLI");

        if (useGatewayApi) {
            return executeViaGateway(agentId, prompt, context, timeout);
        } else {
            return executeViaCli(agentId, prompt, context, timeout);
        }
    }

    /**
     * 通过Gateway API执行Agent（推荐方式）
     */
    private String executeViaGateway(String agentId, String prompt, NodeExecutionContext context, int timeout) throws Exception {
        OpenClawGatewayClient client = new OpenClawGatewayClient(gatewayUrl, gatewayToken);

        // 构建会话Key（实现会话隔离）
        String sessionContext = String.format("%s_%s_%s",
                context.getWorkflowId(),
                context.getExecutionId(),
                context.getNode().getId());

        OpenClawGatewayClient.AgentRequest request = OpenClawGatewayClient.AgentRequest.builder()
                .agentId(agentId)
                .message(prompt)
                .context(sessionContext)
                .build();

        long startTime = System.currentTimeMillis();
        OpenClawGatewayClient.AgentResponse response = client.executeAgent(request);
        long duration = System.currentTimeMillis() - startTime;

        logger.info("Gateway API调用完成，耗时: {}ms, Tokens: {}", duration, response.getTotalTokens());

        if (!response.isSuccess()) {
            throw new RuntimeException("Agent执行失败: " + response.getErrorMessage());
        }

        return response.getContent();
    }

    /**
     * 通过CLI执行Agent（向后兼容）
     */
    private String executeViaCli(String agentId, String prompt, NodeExecutionContext context, int timeout) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(openclawCommand);
        command.add("agent");
        command.add("--agent");
        command.add(agentId);
        command.add("--message");
        command.add(prompt);
        command.add("--local");
        command.add("--json");
        command.add("--timeout");
        command.add(String.valueOf(timeout));

        String sessionId = context.getExecutionId();
        if (sessionId != null) {
            command.add("--session-id");
            command.add(sessionId);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean completed = process.waitFor(timeout, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("Agent执行超时");
        }

        return output.toString().trim();
    }

    /**
     * 获取默认超时时间
     */
    public int getDefaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    // ==================== 配置方法 ====================

    public void setOpenclawCommand(String openclawCommand) {
        this.openclawCommand = openclawCommand;
    }

    public void setUseGatewayApi(boolean useGatewayApi) {
        this.useGatewayApi = useGatewayApi;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public void setGatewayToken(String gatewayToken) {
        this.gatewayToken = gatewayToken;
    }
}