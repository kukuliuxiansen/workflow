package com.openclaw.workflow.engine;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 执行控制对象
 *
 * 用于在 WorkflowEngine 和各个节点处理器之间共享执行状态。
 * 支持暂停、恢复、停止操作，可被长时间运行的任务（如智能节点）感知。
 */
public class ExecutionControl {

    /** 是否暂停 */
    private final AtomicBoolean paused = new AtomicBoolean(false);

    /** 是否停止 */
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    /** 执行ID，用于标识当前执行 */
    private String executionId;

    /**
     * 检查是否应该暂停
     */
    public boolean isPaused() {
        return paused.get();
    }

    /**
     * 检查是否应该停止
     */
    public boolean isStopped() {
        return stopped.get();
    }

    /**
     * 检查是否应该中断（暂停或停止）
     */
    public boolean shouldInterrupt() {
        return paused.get() || stopped.get();
    }

    /**
     * 暂停执行
     */
    public void pause() {
        paused.set(true);
    }

    /**
     * 恢复执行
     */
    public void resume() {
        paused.set(false);
    }

    /**
     * 停止执行
     */
    public void stop() {
        stopped.set(true);
        paused.set(false); // 停止时清除暂停状态
    }

    /**
     * 重置状态（用于新的执行）
     */
    public void reset() {
        paused.set(false);
        stopped.set(false);
        executionId = null;
    }

    /**
     * 初始化新的执行
     */
    public void init(String executionId) {
        reset();
        this.executionId = executionId;
    }

    public String getExecutionId() {
        return executionId;
    }

    /**
     * 检查当前执行是否需要保存状态并退出
     * 如果需要暂停，返回 true，调用方应保存状态后退出
     *
     * @return true 表示需要中断并保存状态
     */
    public boolean checkAndSaveState() {
        return paused.get();
    }
}