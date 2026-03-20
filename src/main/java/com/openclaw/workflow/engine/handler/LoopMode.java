package com.openclaw.workflow.engine.handler;

/**
 * 循环节点的执行模式
 */
public enum LoopMode {
    /**
     * 迭代模式：遍历数据列表
     */
    ITERATOR,

    /**
     * 条件模式：Agent决策是否继续
     */
    CONDITION
}