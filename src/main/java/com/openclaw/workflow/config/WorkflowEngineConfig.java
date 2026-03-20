package com.openclaw.workflow.config;

import com.openclaw.workflow.engine.NodeHandlerFactory;
import com.openclaw.workflow.engine.service.NodePromptService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工作流引擎配置
 *
 * 负责配置工作流引擎的核心组件
 */
@Configuration
public class WorkflowEngineConfig {

    /**
     * 配置NodeHandlerFactory
     *
     * 注入NodePromptService以支持可定制的提示词模板
     */
    @Bean
    public NodeHandlerFactory nodeHandlerFactory(NodePromptService promptService) {
        NodeHandlerFactory factory = new NodeHandlerFactory();
        factory.setPromptService(promptService);
        return factory;
    }
}