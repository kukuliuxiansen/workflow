package com.openclaw.workflow.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * DecisionHistory 复合主键
 */
public class DecisionHistoryId implements Serializable {

    private String id;
    private Integer iteration;

    public DecisionHistoryId() {}

    public DecisionHistoryId(String id, Integer iteration) {
        this.id = id;
        this.iteration = iteration;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Integer getIteration() { return iteration; }
    public void setIteration(Integer iteration) { this.iteration = iteration; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DecisionHistoryId that = (DecisionHistoryId) o;
        return Objects.equals(id, that.id) && Objects.equals(iteration, that.iteration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, iteration);
    }
}