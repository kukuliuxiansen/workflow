package com.openclaw.workflow.engine.smartdecompose.v2;

import com.openclaw.workflow.engine.handler.BaseNodeHandler;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.engine.smartdecompose.v2.config.SmartDecomposeConfig;
import com.openclaw.workflow.engine.smartdecompose.v2.model.DecomposeContext;
import com.openclaw.workflow.engine.smartdecompose.v2.model.SubTask;
import com.openclaw.workflow.engine.smartdecompose.v2.model.enums.DecomposeStatus;
import com.openclaw.workflow.entity.PromptTemplate;
import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.repository.PromptTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SmartDecompose 节点处理器
 *
 * 工作流引擎的入口类，负责初始化上下文、加载模板、协调执行。
 */
@Component
public class SmartDecomposeHandler extends BaseNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(SmartDecomposeHandler.class);
    private static final String NODE_TYPE = "smart_decompose";

    @Autowired
    private DecomposeOrchestrator orchestrator;

    @Autowired
    private PromptTemplateRepository promptTemplateRepository;

    public String getNodeType() {
        return NODE_TYPE;
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) throws Exception {
        logger.info("SmartDecompose 开始执行: executionId={}", ctx.getExecutionId());

        try {
            DecomposeContext context = initializeContext(ctx);
            loadTemplates(context);
            orchestrator.run(context);
            return buildResult(context);
        } catch (Exception e) {
            logger.error("SmartDecompose 执行异常: {}", e.getMessage(), e);
            return NodeResult.failed(e.getMessage());
        }
    }

    private DecomposeContext initializeContext(NodeExecutionContext ctx) {
        DecomposeContext context = new DecomposeContext();
        context.setExecutionId(ctx.getExecutionId());
        context.setWorkflowId(ctx.getWorkflowId());
        context.setNodeId(ctx.getNode().getId());
        context.setProjectPath(ctx.getProjectPath());
        context.setStatus(DecomposeStatus.RUNNING);

        SmartDecomposeConfig config = SmartDecomposeConfig.fromJson(ctx.getNode().getConfig());
        context.setMaxRetries(config.getMaxRetries());
        context.setMaxIterations(config.getMaxIterations());
        context.setRequireManualReview(config.isRequireManualReview());
        context.setDecisionTemplateId(config.getDecisionTemplateId());
        context.setReviewTemplateId(config.getReviewTemplateId());

        String taskDescription = getTaskDescription(ctx);
        SubTask rootTask = SubTask.builder()
            .id("TASK_ROOT")
            .description(taskDescription)
            .depth(0)
            .build();

        context.getTaskQueue().add(rootTask);
        logger.debug("初始化完成: taskDescription={}", taskDescription);

        return context;
    }

    private String getTaskDescription(NodeExecutionContext ctx) {
        Map<String, Object> inputData = ctx.getInput();
        if (inputData != null && inputData.containsKey("task_description")) {
            return String.valueOf(inputData.get("task_description"));
        }
        return "";
    }

    private void loadTemplates(DecomposeContext context) {
        // 加载决策模板
        PromptTemplate decisionTemplate = loadTemplateById(context.getDecisionTemplateId(), "decision");
        context.setDecisionTemplateContent(decisionTemplate.getContent());

        // 加载审核模板
        PromptTemplate reviewTemplate = loadTemplateById(context.getReviewTemplateId(), "review");
        context.setReviewTemplateContent(reviewTemplate.getContent());

        // 加载重试模板
        PromptTemplate retryTemplate = promptTemplateRepository.findByTypeAndIsDefaultTrue("retry")
            .orElseThrow(() -> new IllegalStateException("未找到重试提示词模板"));
        context.setRetryTemplateContent(retryTemplate.getContent());

        logger.debug("模板加载完成: decision={}, review={}, retry={}",
            decisionTemplate.getId(), reviewTemplate.getId(), retryTemplate.getId());
    }

    /**
     * 加载模板：优先使用指定ID，否则使用默认模板
     */
    private PromptTemplate loadTemplateById(String templateId, String type) {
        if (templateId != null && !templateId.isEmpty()) {
            return promptTemplateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalStateException("模板不存在: " + templateId));
        }
        return promptTemplateRepository.findByTypeAndIsDefaultTrue(type)
            .orElseThrow(() -> new IllegalStateException("未找到" + type + "提示词模板"));
    }

    private NodeResult buildResult(DecomposeContext context) {
        switch (context.getStatus()) {
            case COMPLETED:
                Map<String, Object> successData = new HashMap<>();
                successData.put("status", "COMPLETED");
                successData.put("iterations", context.getIterationCount());
                successData.put("completedTasks", context.getCompletedTasks().size());
                successData.put("failedTasks", context.getFailedTasks().size());
                logger.info("执行完成: iterations={}", context.getIterationCount());
                return NodeResult.success(successData);

            case WAITING_MANUAL_REVIEW:
                Map<String, Object> waitingData = new HashMap<>();
                waitingData.put("manualReviewId", context.getManualReviewId());
                waitingData.put("taskId", context.getCurrentTask() != null ? context.getCurrentTask().getId() : null);
                logger.info("等待人工审核: manualReviewId={}", context.getManualReviewId());
                return NodeResult.waiting("等待人工审核", waitingData);

            case FAILED:
            case ITERATION_EXCEEDED:
            default:
                logger.warn("执行失败: status={}, error={}", context.getStatus(), context.getErrorMessage());
                return NodeResult.failed(context.getErrorMessage());
        }
    }

    @Override
    public List<String> validate(WorkflowNode node) {
        List<String> errors = new ArrayList<>();

        try {
            SmartDecomposeConfig config = SmartDecomposeConfig.fromJson(node.getConfig());

            if (config.getMaxRetries() < 1 || config.getMaxRetries() > 20) {
                errors.add("maxRetries 必须在 1-20 之间，当前值: " + config.getMaxRetries());
            }

            if (config.getMaxIterations() < 1 || config.getMaxIterations() > 100) {
                errors.add("maxIterations 必须在 1-100 之间，当前值: " + config.getMaxIterations());
            }

        } catch (Exception e) {
            errors.add("配置格式错误: " + e.getMessage());
        }

        return errors;
    }
}