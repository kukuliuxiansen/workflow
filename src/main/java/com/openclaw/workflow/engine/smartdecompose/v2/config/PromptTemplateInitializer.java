package com.openclaw.workflow.engine.smartdecompose.v2.config;

import com.openclaw.workflow.entity.PromptTemplate;
import com.openclaw.workflow.repository.PromptTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 提示词模板初始化器
 *
 * 应用启动时自动初始化默认模板到数据库。
 */
@Component
public class PromptTemplateInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(PromptTemplateInitializer.class);

    @Autowired
    private PromptTemplateRepository templateRepository;

    @Autowired
    private ResourceLoader resourceLoader;

    @Override
    public void run(ApplicationArguments args) {
        logger.info("初始化提示词模板...");
        initDecisionTemplate();
        initReviewTemplate();
        initRetryTemplate();
        logger.info("提示词模板初始化完成");
    }

    private void initDecisionTemplate() {
        if (templateRepository.findByTypeAndIsDefaultTrue("decision").isPresent()) {
            logger.debug("决策模板已存在，跳过初始化");
            return;
        }

        String content = loadTemplate("classpath:templates/decision-template.md");
        if (content == null) {
            logger.warn("无法加载决策模板文件，使用内置模板");
            content = getBuiltinDecisionTemplate();
        }

        PromptTemplate template = PromptTemplate.decisionTemplate(
            "tpl_decision_default",
            "默认决策模板",
            content
        );
        templateRepository.save(template);
        logger.info("决策模板初始化完成");
    }

    private void initReviewTemplate() {
        if (templateRepository.findByTypeAndIsDefaultTrue("review").isPresent()) {
            logger.debug("审核模板已存在，跳过初始化");
            return;
        }

        String content = loadTemplate("classpath:templates/review-template.md");
        if (content == null) {
            logger.warn("无法加载审核模板文件，使用内置模板");
            content = getBuiltinReviewTemplate();
        }

        PromptTemplate template = PromptTemplate.reviewTemplate(
            "tpl_review_default",
            "默认审核模板",
            content
        );
        templateRepository.save(template);
        logger.info("审核模板初始化完成");
    }

    private void initRetryTemplate() {
        if (templateRepository.findByTypeAndIsDefaultTrue("retry").isPresent()) {
            logger.debug("重试模板已存在，跳过初始化");
            return;
        }

        String content = loadTemplate("classpath:templates/retry-template.md");
        if (content == null) {
            logger.warn("无法加载重试模板文件，使用内置模板");
            content = getBuiltinRetryTemplate();
        }

        PromptTemplate template = PromptTemplate.retryTemplate(
            "tpl_retry_default",
            "默认重试模板",
            content
        );
        templateRepository.save(template);
        logger.info("重试模板初始化完成");
    }

    private String loadTemplate(String location) {
        try {
            Resource resource = resourceLoader.getResource(location);
            if (resource.exists()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            }
        } catch (Exception e) {
            logger.error("加载模板失败: {}", location, e);
        }
        return null;
    }

    private String getBuiltinDecisionTemplate() {
        return "你是一个智能任务执行引擎。请分析并处理以下任务。\n\n" +
            "项目路径: {{projectPath}}\n" +
            "当前任务: {{taskDescription}}\n" +
            "已完成任务: {{completedTasks}}\n\n" +
            "决策规则：\n" +
            "- 预估 8 分钟内可完成 → execute\n" +
            "- 需要多步骤 → split\n\n" +
            "输出 JSON 格式：\n" +
            "{ \"decision\": \"execute|split\", \"thought\": \"分析\", \"result\": \"结果\" 或 \"tasks\": [...] }";
    }

    private String getBuiltinReviewTemplate() {
        return "你是一个审核员。请审核以下任务执行结果。\n\n" +
            "任务: {{taskDescription}}\n" +
            "验收标准: {{criteria}}\n" +
            "执行结果: {{executionResult}}\n" +
            "项目路径: {{projectPath}}\n\n" +
            "输出 JSON：\n" +
            "{ \"status\": \"APPROVED|REJECTED\", \"thought\": \"分析\", \"issues\": [...] }";
    }

    private String getBuiltinRetryTemplate() {
        return "请重新执行任务并解决问题。\n\n" +
            "任务: {{taskDescription}}\n" +
            "问题: {{previousIssues}}\n" +
            "项目路径: {{projectPath}}\n\n" +
            "输出 JSON：\n" +
            "{ \"decision\": \"execute\", \"thought\": \"分析\", \"result\": \"结果\" }";
    }
}