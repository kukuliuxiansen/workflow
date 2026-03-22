package com.openclaw.workflow.engine.smartdecompose.v2.client;

import com.openclaw.workflow.engine.connector.AgentRequest;
import com.openclaw.workflow.engine.connector.AgentResponse;
import com.openclaw.workflow.engine.connector.OpenClawGatewayClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * OpenClaw 客户端
 *
 * 负责调用 OpenClaw Gateway，管理会话。
 * 所有与 OpenClaw 的交互都通过此类进行。
 */
@Component
public class OpenClawClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenClawClient.class);

    @Value("${openclaw.gateway.url:http://localhost:18789}")
    private String gatewayUrl;

    @Value("${openclaw.gateway.token:56b640cc2d91411f63255af68355c19ee33c88ec458878ca}")
    private String token;

    @Value("${openclaw.agent.id:smart-decompose}")
    private String agentId;

    /** 当前会话ID */
    private String currentSessionId;

    /** Gateway 客户端实例 */
    private OpenClawGatewayClient gatewayClient;

    /**
     * 获取 Gateway 客户端（延迟初始化）
     */
    private OpenClawGatewayClient getGatewayClient() {
        if (gatewayClient == null) {
            gatewayClient = new OpenClawGatewayClient(gatewayUrl, token);
        }
        return gatewayClient;
    }

    /**
     * 执行决策/任务
     *
     * @param prompt 完整的决策提示词
     * @return OpenClaw 的原始文本响应
     * @throws OpenClawException 调用失败时抛出
     */
    public String execute(String prompt) {
        return callAgent(prompt);
    }

    /**
     * 执行审核
     *
     * @param prompt 完整的审核提示词
     * @return OpenClaw 的原始文本响应
     * @throws OpenClawException 调用失败时抛出
     */
    public String review(String prompt) {
        return callAgent(prompt);
    }

    /**
     * 调用 OpenClaw Agent
     *
     * 入参: String prompt
     * 出参: String (OpenClaw 的原始文本响应)
     *
     * @param prompt 提示词
     * @return OpenClaw 的原始文本响应
     * @throws OpenClawException 调用失败时抛出
     */
    private String callAgent(String prompt) {
        logger.info("[OPENCLAW] ===== 调用 OpenClaw =====");
        logger.info("[OPENCLAW] 入参 - agentId: {}", agentId);
        logger.info("[OPENCLAW] 入参 - currentSessionId: {}", currentSessionId);
        logger.info("[OPENCLAW] 入参 - prompt长度: {} 字符", prompt != null ? prompt.length() : 0);
        logger.info("[OPENCLAW] 入参 - prompt内容:\n{}", truncate(prompt, 500));

        try {
            AgentRequest.Builder builder = AgentRequest.builder()
                .agentId(agentId)
                .message(prompt);

            if (currentSessionId != null && !currentSessionId.isEmpty()) {
                builder.sessionKey(currentSessionId);
                logger.info("[OPENCLAW] 使用已有会话: {}", currentSessionId);
            } else {
                logger.info("[OPENCLAW] 创建新会话");
            }

            AgentRequest request = builder.build();
            logger.info("[OPENCLAW] 发送请求到 Gateway: {}", gatewayUrl);

            long startTime = System.currentTimeMillis();
            AgentResponse response = getGatewayClient().executeAgent(request);
            long elapsed = System.currentTimeMillis() - startTime;

            logger.info("[OPENCLAW] Gateway 响应耗时: {}ms", elapsed);
            logger.info("[OPENCLAW] Gateway 响应状态: {}", response.isSuccess() ? "成功" : "失败");

            if (!response.isSuccess()) {
                logger.error("[OPENCLAW] 执行失败: {}", response.getErrorMessage());
                throw new OpenClawException(
                    OpenClawException.ErrorCode.SERVER_ERROR,
                    response.getErrorMessage()
                );
            }

            // 保存会话ID以便后续复用
            String newSessionId = response.getSessionKey();
            logger.info("[OPENCLAW] 返回的 sessionId: {}", newSessionId);
            currentSessionId = newSessionId;

            String content = response.getContent();
            logger.info("[OPENCLAW] 返回内容长度: {} 字符", content != null ? content.length() : 0);
            logger.info("[OPENCLAW] 返回内容:\n{}", truncate(content, 1000));
            logger.info("[OPENCLAW] ===== OpenClaw 调用完成 =====");

            return content;

        } catch (OpenClawException e) {
            logger.error("[OPENCLAW] OpenClaw 异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("[OPENCLAW] 连接异常: {}", e.getMessage(), e);
            throw new OpenClawException(
                OpenClawException.ErrorCode.CONNECTION_FAILED,
                "调用 OpenClaw Gateway 失败: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * 开始新会话
     *
     * 清除当前会话ID，下次调用将创建新会话
     */
    public void startSession() {
        logger.info("[OPENCLAW] 开始新会话 (清除旧 sessionId: {})", currentSessionId);
        this.currentSessionId = null;
    }

    /**
     * 结束会话
     *
     * 清除会话ID，关闭会话（如果 Gateway 支持）
     */
    public void endSession() {
        logger.info("[OPENCLAW] 结束会话 (当前 sessionId: {})", currentSessionId);
        this.currentSessionId = null;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "null";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen) + "...";
    }

    /**
     * 获取当前会话ID
     *
     * @return 当前会话ID，无会话返回 null
     */
    public String getSessionId() {
        return currentSessionId;
    }

    /**
     * 设置会话ID
     *
     * 用于恢复执行时复用会话
     *
     * @param sessionId 会话ID
     */
    public void setSessionId(String sessionId) {
        this.currentSessionId = sessionId;
    }

    /**
     * 设置 Agent ID
     *
     * @param agentId Agent ID
     */
    public void setAgentId(String agentId) {
        this.agentId = agentId;
        // 重置客户端以使用新的agentId
        this.gatewayClient = null;
    }

    /**
     * 获取当前 Agent ID
     */
    public String getAgentId() {
        return agentId;
    }
}