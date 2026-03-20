package com.openclaw.workflow.engine.connector;

/**
 * 流式响应监听器
 */
public interface StreamListener {
    /**
     * 收到增量内容
     */
    void onChunk(String delta);

    /**
     * 流式结束
     */
    void onComplete(AgentResponse response);

    /**
     * 发生错误
     */
    void onError(Exception e);
}