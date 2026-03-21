package com.openclaw.workflow.engine.smartdecompose.v2.extension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 扩展注册中心
 *
 * 管理所有扩展组件的注册和获取。
 */
@Component
public class ExtensionRegistry {

    private final List<DecisionHandler> decisionHandlers = new ArrayList<>();
    private final List<TaskSplitInterceptor> splitInterceptors = new ArrayList<>();

    private ReviewStrategy reviewStrategy;

    @Autowired(required = false)
    public void setDecisionHandlers(List<DecisionHandler> handlers) {
        this.decisionHandlers.addAll(handlers);
    }

    @Autowired(required = false)
    public void setSplitInterceptors(List<TaskSplitInterceptor> interceptors) {
        this.splitInterceptors.addAll(interceptors);
    }

    @Autowired(required = false)
    public void setReviewStrategy(ReviewStrategy strategy) {
        this.reviewStrategy = strategy;
    }

    /**
     * 获取所有决策处理器
     */
    public List<DecisionHandler> getDecisionHandlers() {
        return decisionHandlers;
    }

    /**
     * 获取所有拆分拦截器
     */
    public List<TaskSplitInterceptor> getSplitInterceptors() {
        return splitInterceptors;
    }

    /**
     * 获取审核策略
     */
    public ReviewStrategy getReviewStrategy() {
        return reviewStrategy;
    }

    /**
     * 注册决策处理器
     */
    public void registerDecisionHandler(DecisionHandler handler) {
        decisionHandlers.add(handler);
    }

    /**
     * 注册拆分拦截器
     */
    public void registerSplitInterceptor(TaskSplitInterceptor interceptor) {
        splitInterceptors.add(interceptor);
    }

    /**
     * 设置审核策略
     */
    public void setStrategy(ReviewStrategy strategy) {
        this.reviewStrategy = strategy;
    }
}