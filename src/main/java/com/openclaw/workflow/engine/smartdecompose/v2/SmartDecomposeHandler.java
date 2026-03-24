package com.openclaw.workflow.engine.smartdecompose.v2;

import com.openclaw.workflow.engine.handler.BaseNodeHandler;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.engine.smartdecompose.v2.client.OpenClawClient;
import com.openclaw.workflow.engine.smartdecompose.v2.config.SmartDecomposeConfig;
import com.openclaw.workflow.engine.smartdecompose.v2.model.DecomposeContext;
import com.openclaw.workflow.engine.smartdecompose.v2.model.SubTask;
import com.openclaw.workflow.engine.smartdecompose.v2.model.enums.DecomposeStatus;
import com.openclaw.workflow.engine.smartdecompose.v2.persistence.StatePersister;
import com.openclaw.workflow.entity.PromptTemplate;
import com.openclaw.workflow.entity.SmartDecomposeScene;
import com.openclaw.workflow.entity.TemplateConfig;
import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.repository.PromptTemplateRepository;
import com.openclaw.workflow.repository.SmartDecomposeSceneRepository;
import com.openclaw.workflow.repository.TemplateConfigRepository;
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

    @Autowired
    private SmartDecomposeSceneRepository sceneRepository;

    @Autowired
    private TemplateConfigRepository templateConfigRepository;

    @Autowired
    private StatePersister statePersister;

    @Autowired
    private OpenClawClient openClawClient;

    public String getNodeType() {
        return NODE_TYPE;
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) throws Exception {
        logger.info("SmartDecompose 开始执行: executionId={}", ctx.getExecutionId());

        try {
            DecomposeContext context = tryRestoreOrCreate(ctx);
            loadTemplates(context);
            // 传递 ExecutionControl 以支持暂停/停止
            orchestrator.run(context, ctx.getExecutionControl());
            return buildResult(context);
        } catch (Exception e) {
            logger.error("SmartDecompose 执行异常: {}", e.getMessage(), e);
            return NodeResult.failed(e.getMessage());
        }
    }

    /**
     * 尝试恢复或创建新的上下文
     */
    private DecomposeContext tryRestoreOrCreate(NodeExecutionContext ctx) {
        String executionId = ctx.getExecutionId();
        String nodeId = ctx.getNode().getId();

        // 查找备份数据
        DecomposeContext cached = statePersister.loadIfExists(executionId, nodeId);

        if (cached != null && (cached.getStatus() == DecomposeStatus.RUNNING || cached.getStatus() == DecomposeStatus.PAUSED)) {
            logger.info("[HANDLER] 恢复未完成的任务: executionId={}, status={}, sessionId={}",
                executionId, cached.getStatus(), cached.getOpenClawSessionId());

            // 恢复OpenClaw会话
            if (cached.getOpenClawSessionId() != null) {
                openClawClient.setSessionId(cached.getOpenClawSessionId());
            }

            // 如果是暂停状态，重置为运行状态
            if (cached.getStatus() == DecomposeStatus.PAUSED) {
                cached.setStatus(DecomposeStatus.RUNNING);
                logger.info("[HANDLER] 暂停状态恢复为运行状态");
            }

            return cached;
        }

        // 没有备份，正常初始化
        logger.info("[HANDLER] 创建新的执行上下文");
        return initializeContext(ctx);
    }

    private DecomposeContext initializeContext(NodeExecutionContext ctx) {
        DecomposeContext context = new DecomposeContext();
        context.setExecutionId(ctx.getExecutionId());
        context.setWorkflowId(ctx.getWorkflowId());
        context.setNodeId(ctx.getNode().getId());
        context.setProjectPath(ctx.getProjectPath());
        context.setStatus(DecomposeStatus.RUNNING);

        SmartDecomposeConfig config = SmartDecomposeConfig.fromJson(ctx.getNode().getConfig());

        // 加载模板配置（优先使用 templateId）
        if (config.getTemplateId() != null && !config.getTemplateId().isEmpty()) {
            loadTemplateConfig(context, config.getTemplateId());
        }

        // 加载场景配置（如果指定了sceneId）
        if (config.getSceneId() != null && !config.getSceneId().isEmpty()) {
            loadSceneConfig(context, config.getSceneId());
        }

        // 节点配置覆盖默认值
        context.setMaxRetries(config.getMaxRetries());
        context.setMaxTotalTasks(config.getMaxTotalTasks());
        context.setMaxIterations(config.getMaxIterations());
        context.setRequireManualReview(config.isRequireManualReview());
        context.setTemplateId(config.getTemplateId());

        // 如果场景未设置模板ID，使用节点配置（向后兼容）
        if (context.getDecisionTemplateId() == null) {
            context.setDecisionTemplateId(config.getDecisionTemplateId());
        }
        if (context.getReviewTemplateId() == null) {
            context.setReviewTemplateId(config.getReviewTemplateId());
        }

        // 任务描述优先使用节点配置，否则从上下文获取
        String taskDescription = config.getTaskDescription();
        if (taskDescription == null || taskDescription.isEmpty()) {
            taskDescription = getTaskDescription(ctx);
        }

        SubTask rootTask = SubTask.builder()
            .id("TASK_ROOT")
            .description(taskDescription)
            .depth(0)
            .build();

        context.getTaskQueue().add(rootTask);
        logger.debug("初始化完成: taskDescription={}, templateId={}", taskDescription, config.getTemplateId());

        return context;
    }

    /**
     * 加载模板配置
     */
    private void loadTemplateConfig(DecomposeContext context, String templateId) {
        TemplateConfig template = templateConfigRepository.findById(templateId).orElse(null);
        if (template == null) {
            logger.warn("模板不存在: {}, 使用默认配置", templateId);
            return;
        }

        logger.info("加载模板配置: id={}, name={}", template.getId(), template.getName());

        // 设置模板内容
        if (template.getDecisionTemplate() != null) {
            context.setDecisionTemplateContent(template.getDecisionTemplate());
        }
        if (template.getReviewTemplate() != null) {
            context.setReviewTemplateContent(template.getReviewTemplate());
        }
        if (template.getRetryTemplate() != null) {
            context.setRetryTemplateContent(template.getRetryTemplate());
        }
    }

    /**
     * 加载场景配置
     */
    private void loadSceneConfig(DecomposeContext context, String sceneId) {
        SmartDecomposeScene scene = sceneRepository.findById(sceneId)
            .orElse(null);

        if (scene == null) {
            logger.warn("场景不存在: {}, 使用默认配置", sceneId);
            return;
        }

        logger.info("加载场景配置: id={}, name={}", scene.getId(), scene.getName());

        // 模板ID
        if (scene.getDecisionTemplateId() != null) {
            context.setDecisionTemplateId(scene.getDecisionTemplateId());
        }
        if (scene.getReviewTemplateId() != null) {
            context.setReviewTemplateId(scene.getReviewTemplateId());
        }
        if (scene.getRetryTemplateId() != null) {
            context.setRetryTemplateId(scene.getRetryTemplateId());
        }

        // 场景参数
        if (scene.getDecisionThresholdMinutes() != null) {
            context.setDecisionThresholdMinutes(scene.getDecisionThresholdMinutes());
        }
        if (scene.getOutputFormatExecute() != null) {
            context.setOutputFormatExecute(scene.getOutputFormatExecute());
        }
        if (scene.getOutputFormatSplit() != null) {
            context.setOutputFormatSplit(scene.getOutputFormatSplit());
        }

        context.setSceneId(sceneId);
    }

    private String getTaskDescription(NodeExecutionContext ctx) {
        Map<String, Object> inputData = ctx.getInput();
        if (inputData != null && inputData.containsKey("task_description")) {
            return String.valueOf(inputData.get("task_description"));
        }
        return "";
    }

    private void loadTemplates(DecomposeContext context) {
        // 只有当模板内容为空时才从 PromptTemplate 加载（避免覆盖 loadTemplateConfig 设置的内容）
        if (context.getDecisionTemplateContent() == null || context.getDecisionTemplateContent().isEmpty()) {
            PromptTemplate decisionTemplate = loadTemplateById(context.getDecisionTemplateId(), "decision");
            context.setDecisionTemplateContent(decisionTemplate.getContent());
        }

        // 加载审核模板
        if (context.getReviewTemplateContent() == null || context.getReviewTemplateContent().isEmpty()) {
            PromptTemplate reviewTemplate = loadTemplateById(context.getReviewTemplateId(), "review");
            context.setReviewTemplateContent(reviewTemplate.getContent());
        }

        // 加载重试模板（优先使用场景配置的ID）
        if (context.getRetryTemplateContent() == null || context.getRetryTemplateContent().isEmpty()) {
            PromptTemplate retryTemplate;
            if (context.getRetryTemplateId() != null && !context.getRetryTemplateId().isEmpty()) {
                retryTemplate = promptTemplateRepository.findById(context.getRetryTemplateId())
                    .orElseThrow(() -> new IllegalStateException("重试模板不存在: " + context.getRetryTemplateId()));
            } else {
                retryTemplate = promptTemplateRepository.findByTypeAndIsDefaultTrue("retry")
                    .orElseThrow(() -> new IllegalStateException("未找到重试提示词模板"));
            }
            context.setRetryTemplateContent(retryTemplate.getContent());
        }

        logger.debug("模板加载完成: decision={}, review={}, retry={}",
            context.getDecisionTemplateId() != null ? context.getDecisionTemplateId() : "default",
            context.getReviewTemplateId() != null ? context.getReviewTemplateId() : "default",
            context.getRetryTemplateId() != null ? context.getRetryTemplateId() : "default");
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

            case PAUSED:
                Map<String, Object> pausedData = new HashMap<>();
                pausedData.put("status", "PAUSED");
                pausedData.put("openClawSessionId", context.getOpenClawSessionId());
                pausedData.put("iterations", context.getIterationCount());
                pausedData.put("remainingTasks", context.getTaskQueue().size());
                pausedData.put("completedTasks", context.getCompletedTasks().size());
                logger.info("执行暂停: iterations={}, remainingTasks={}", context.getIterationCount(), context.getTaskQueue().size());
                return NodeResult.paused("执行已暂停", pausedData);

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