package com.openclaw.workflow.engine.smartdecompose.v2.client;

import com.openclaw.workflow.engine.connector.AgentRequest;
import com.openclaw.workflow.engine.connector.AgentResponse;
import com.openclaw.workflow.engine.connector.OpenClawGatewayClient;
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

    @Value("${openclaw.gateway.url:http://localhost:18789}")
    private String gatewayUrl;

    @Value("${openclaw.gateway.token:}")
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
     * @param prompt 提示词
     * @return OpenClaw 的原始文本响应
     * @throws OpenClawException 调用失败时抛出
     */
    private String callAgent(String prompt) {
        try {
            AgentRequest.Builder builder = AgentRequest.builder()
                .agentId(agentId)
                .message(prompt);

            if (currentSessionId != null && !currentSessionId.isEmpty()) {
                builder.sessionKey(currentSessionId);
            }

            AgentResponse response = getGatewayClient().executeAgent(builder.build());

            if (!response.isSuccess()) {
                throw new OpenClawException(
                    OpenClawException.ErrorCode.SERVER_ERROR,
                    response.getErrorMessage()
                );
            }

            // 保存会话ID以便后续复用
            currentSessionId = response.getSessionKey();

            return response.getContent();

        } catch (OpenClawException e) {
            throw e;
        } catch (Exception e) {
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
        // 实现思路：将 currentSessionId 设为 null
        this.currentSessionId = null;
    }

    /**
     * 结束会话
     *
     * 清除会话ID，关闭会话（如果 Gateway 支持）
     */
    public void endSession() {
        // 实现思路：
        // 1. 如果 currentSessionId 不为空，调用 Gateway 关闭会话
        // 2. 将 currentSessionId 设为 null

        this.currentSessionId = null;
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
}