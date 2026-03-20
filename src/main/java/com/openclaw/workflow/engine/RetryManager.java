package com.openclaw.workflow.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 重试管理器
 */
public class RetryManager {

    private final Map<String, Integer> nodeRetryCount = new ConcurrentHashMap<>();
    private int globalRetryCount = 0;
    private int globalLoopCount = 0;

    private final int maxNodeRetries;
    private final int maxGlobalRetries;
    private final int maxLoopIterations;

    public RetryManager() {
        this(3, 10, 100);
    }

    public RetryManager(int maxNodeRetries, int maxGlobalRetries, int maxLoopIterations) {
        this.maxNodeRetries = maxNodeRetries;
        this.maxGlobalRetries = maxGlobalRetries;
        this.maxLoopIterations = maxLoopIterations;
    }

    public void incrementNodeRetry(String nodeId) {
        nodeRetryCount.merge(nodeId, 1, Integer::sum);
    }

    public void incrementGlobalRetry() {
        globalRetryCount++;
    }

    public void incrementLoopCount() {
        globalLoopCount++;
    }

    public boolean checkNodeRetryLimit(String nodeId) {
        int count = nodeRetryCount.getOrDefault(nodeId, 0);
        return count < maxNodeRetries;
    }

    public boolean checkGlobalRetryLimit() {
        return globalRetryCount < maxGlobalRetries;
    }

    public boolean checkLoopLimit() {
        return globalLoopCount < maxLoopIterations;
    }

    public boolean checkAllLimits(String nodeId) {
        return checkNodeRetryLimit(nodeId)
                && checkGlobalRetryLimit()
                && checkLoopLimit();
    }

    public String getLimitError(String nodeId) {
        int nodeCount = nodeRetryCount.getOrDefault(nodeId, 0);
        if (nodeCount >= maxNodeRetries) {
            return "节点重试次数超限: " + nodeCount + "/" + maxNodeRetries;
        }
        if (globalRetryCount >= maxGlobalRetries) {
            return "全局重试次数超限: " + globalRetryCount + "/" + maxGlobalRetries;
        }
        if (globalLoopCount >= maxLoopIterations) {
            return "循环次数超限: " + globalLoopCount + "/" + maxLoopIterations;
        }
        return null;
    }

    public void reset() {
        nodeRetryCount.clear();
        globalRetryCount = 0;
        globalLoopCount = 0;
    }

    public int getNodeRetryCount(String nodeId) {
        return nodeRetryCount.getOrDefault(nodeId, 0);
    }

    public int getGlobalRetryCount() {
        return globalRetryCount;
    }

    public int getGlobalLoopCount() {
        return globalLoopCount;
    }
}