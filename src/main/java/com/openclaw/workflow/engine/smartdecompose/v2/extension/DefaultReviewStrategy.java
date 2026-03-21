package com.openclaw.workflow.engine.smartdecompose.v2.extension;

import com.openclaw.workflow.engine.smartdecompose.v2.model.SubTask;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 默认审核策略
 */
@Component
public class DefaultReviewStrategy implements ReviewStrategy {

    @Override
    public boolean requireManualReview(SubTask task, int retryCount) {
        // 默认：重试次数超过 3 次时需要人工审核
        return retryCount >= 3;
    }

    @Override
    public String buildRetryPrompt(SubTask task, List<String> issues) {
        StringBuilder sb = new StringBuilder();
        sb.append("请重新执行任务并解决以下问题：\n\n");
        sb.append("任务：").append(task.getDescription()).append("\n\n");
        sb.append("发现的问题：\n");
        sb.append(issues.stream()
            .map(s -> "- " + s)
            .collect(Collectors.joining("\n")));
        sb.append("\n\n请确保解决所有问题后重新执行。");
        return sb.toString();
    }

    @Override
    public String getName() {
        return "DefaultReviewStrategy";
    }
}