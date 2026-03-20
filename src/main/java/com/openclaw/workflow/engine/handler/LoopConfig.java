package com.openclaw.workflow.engine.handler;

import java.util.ArrayList;
import java.util.List;

/**
 * 循环节点配置
 */
public class LoopConfig {

    private static final int DEFAULT_MAX_ITERATIONS = 100;

    private LoopMode loopMode = LoopMode.CONDITION;
    private int maxIterations = DEFAULT_MAX_ITERATIONS;
    private String loopVariable = "item";
    private List<Object> iteratorSource;
    private String loopBodyEntryNode;
    private String loopBodyExitNode;
    private String exitCondition;
    private String customPrompt;

    public LoopMode getLoopMode() {
        return loopMode;
    }

    public void setLoopMode(LoopMode loopMode) {
        this.loopMode = loopMode;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public String getLoopVariable() {
        return loopVariable;
    }

    public void setLoopVariable(String loopVariable) {
        this.loopVariable = loopVariable;
    }

    public List<Object> getIteratorSource() {
        return iteratorSource;
    }

    public void setIteratorSource(List<Object> iteratorSource) {
        this.iteratorSource = iteratorSource;
    }

    public String getLoopBodyEntryNode() {
        return loopBodyEntryNode;
    }

    public void setLoopBodyEntryNode(String loopBodyEntryNode) {
        this.loopBodyEntryNode = loopBodyEntryNode;
    }

    public String getLoopBodyExitNode() {
        return loopBodyExitNode;
    }

    public void setLoopBodyExitNode(String loopBodyExitNode) {
        this.loopBodyExitNode = loopBodyExitNode;
    }

    public String getExitCondition() {
        return exitCondition;
    }

    public void setExitCondition(String exitCondition) {
        this.exitCondition = exitCondition;
    }

    public String getCustomPrompt() {
        return customPrompt;
    }

    public void setCustomPrompt(String customPrompt) {
        this.customPrompt = customPrompt;
    }
}