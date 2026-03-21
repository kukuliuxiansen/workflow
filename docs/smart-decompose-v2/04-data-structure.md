# SmartDecompose 数据结构设计

## 一、数据库表设计

### 1.1 核心表

```sql
-- 提示词模板表
CREATE TABLE prompt_template (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL,              -- decision / review / retry
    name TEXT NOT NULL,
    content TEXT NOT NULL,
    variables TEXT,                  -- JSON
    is_default INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 执行状态表
CREATE TABLE decompose_execution_state (
    id TEXT PRIMARY KEY,
    execution_id TEXT NOT NULL,
    workflow_id TEXT NOT NULL,
    node_id TEXT NOT NULL,
    status TEXT NOT NULL,            -- RUNNING / COMPLETED / FAILED / WAITING_MANUAL_REVIEW
    project_path TEXT,
    current_task_id TEXT,
    task_queue TEXT,                 -- JSON 序列化的任务队列
    completed_tasks TEXT,            -- JSON
    failed_tasks TEXT,               -- JSON
    iteration_count INTEGER DEFAULT 0,
    max_iterations INTEGER DEFAULT 50,
    max_retries INTEGER DEFAULT 5,
    require_manual_review INTEGER DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 决策历史表
CREATE TABLE decompose_decision_history (
    id TEXT PRIMARY KEY,
    execution_id TEXT NOT NULL,
    iteration INTEGER NOT NULL,
    task_id TEXT NOT NULL,
    task_description TEXT,
    decision TEXT NOT NULL,          -- execute / split
    thought TEXT,
    result TEXT,
    subtasks TEXT,                   -- JSON
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 子任务表
CREATE TABLE decompose_subtask (
    id TEXT PRIMARY KEY,
    execution_id TEXT NOT NULL,
    parent_task_id TEXT,
    description TEXT NOT NULL,
    criteria TEXT,
    depth INTEGER DEFAULT 0,
    status TEXT DEFAULT 'PENDING',   -- PENDING / RUNNING / COMPLETED / FAILED
    execution_result TEXT,
    review_status TEXT,
    review_issues TEXT,              -- JSON
    retry_count INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME
);

-- 人工审核表
CREATE TABLE manual_review (
    id TEXT PRIMARY KEY,
    execution_id TEXT NOT NULL,
    task_id TEXT NOT NULL,
    task_description TEXT,
    execution_result TEXT,
    review_issues TEXT,              -- JSON
    status TEXT DEFAULT 'WAITING',   -- WAITING / APPROVED / REJECTED
    reviewer TEXT,
    review_comment TEXT,
    reviewed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 提示词模板版本表
CREATE TABLE prompt_template_version (
    id TEXT PRIMARY KEY,
    template_id TEXT NOT NULL,
    version INTEGER NOT NULL,
    content TEXT NOT NULL,
    change_note TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by TEXT,
    FOREIGN KEY (template_id) REFERENCES prompt_template(id)
);

-- 索引
CREATE INDEX idx_execution_state_execution ON decompose_execution_state(execution_id);
CREATE INDEX idx_decision_history_execution ON decompose_decision_history(execution_id);
CREATE INDEX idx_subtask_execution ON decompose_subtask(execution_id);
CREATE INDEX idx_manual_review_status ON manual_review(status);
```

---

## 二、Java 实体类

### 2.1 DecomposeContext

```java
/**
 * 分解执行上下文
 */
public class DecomposeContext {

    private String executionId;
    private String workflowId;
    private String nodeId;
    private String projectPath;
    private String techStack;

    // 任务队列
    private Queue<SubTask> taskQueue;

    // 已完成/失败任务
    private List<SubTask> completedTasks;
    private List<SubTask> failedTasks;

    // 当前任务
    private SubTask currentTask;

    // 迭代控制
    private int iterationCount;
    private int maxIterations;

    // 重试配置
    private int maxRetries;
    private boolean requireManualReview;

    // 状态
    private DecomposeStatus status;
    private String errorMessage;

    // 提示词模板
    private PromptTemplate decisionTemplate;
    private PromptTemplate reviewTemplate;

    // 人工审核ID
    private String manualReviewId;

    public enum DecomposeStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        WAITING_MANUAL_REVIEW,
        ITERATION_EXCEEDED
    }

    // Getters, Setters, Builder...
}
```

### 2.2 SubTask

```java
/**
 * 子任务
 */
public class SubTask {

    private String id;
    private String parentTaskId;
    private String description;
    private String criteria;
    private int depth;
    private int estimatedMinutes;

    // 状态
    private SubTaskStatus status;

    // 执行结果
    private String executionResult;

    // 审核结果
    private ReviewStatus reviewStatus;
    private List<String> reviewIssues;
    private int retryCount;

    public enum SubTaskStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }

    public enum ReviewStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private SubTask task = new SubTask();

        public Builder id(String id) { task.id = id; return this; }
        public Builder parentTaskId(String parentTaskId) { task.parentTaskId = parentTaskId; return this; }
        public Builder description(String description) { task.description = description; return this; }
        public Builder criteria(String criteria) { task.criteria = criteria; return this; }
        public Builder depth(int depth) { task.depth = depth; return this; }
        public Builder estimatedMinutes(int minutes) { task.estimatedMinutes = minutes; return this; }

        public SubTask build() {
            task.status = SubTaskStatus.PENDING;
            task.reviewStatus = ReviewStatus.PENDING;
            task.retryCount = 0;
            return task;
        }
    }
}
```

### 2.3 DecisionResponse

```java
/**
 * 决策响应
 */
public class DecisionResponse {

    private String decision;     // "execute" 或 "split"
    private String thought;
    private String result;       // decision=execute 时
    private List<SubTask> tasks; // decision=split 时

    public boolean isExecute() {
        return "execute".equals(decision);
    }

    public boolean isSplit() {
        return "split".equals(decision);
    }

    // Getters, Setters...
}
```

### 2.4 ReviewResponse

```java
/**
 * 审核响应
 */
public class ReviewResponse {

    private String status;       // "APPROVED" 或 "REJECTED"
    private String thought;
    private String summary;      // status=APPROVED 时
    private List<String> issues; // status=REJECTED 时
    private String suggestion;   // status=REJECTED 时

    public boolean isApproved() {
        return "APPROVED".equals(status);
    }

    public boolean isRejected() {
        return "REJECTED".equals(status);
    }

    // Getters, Setters...
}
```

### 2.5 PromptTemplate

```java
/**
 * 提示词模板
 */
@Entity
@Table(name = "prompt_template")
public class PromptTemplate {

    @Id
    private String id;

    private String type;         // decision / review / retry
    private String name;
    private String content;

    @Column(columnDefinition = "TEXT")
    private String variables;    // JSON

    private boolean isDefault;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters, Setters...
}
```

### 2.6 ManualReviewRecord

```java
/**
 * 人工审核记录
 */
@Entity
@Table(name = "manual_review")
public class ManualReviewRecord {

    @Id
    private String id;

    private String executionId;
    private String taskId;
    private String taskDescription;

    @Column(columnDefinition = "TEXT")
    private String executionResult;

    @Column(columnDefinition = "TEXT")
    private String reviewIssues;   // JSON

    private ManualReviewStatus status;

    private String reviewer;
    private String reviewComment;
    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;

    public enum ManualReviewStatus {
        WAITING,
        APPROVED,
        REJECTED
    }

    // Getters, Setters...
}
```

---

## 三、DTO 类

### 3.1 节点配置

```java
/**
 * SmartDecompose 节点配置
 */
public class SmartDecomposeConfig {

    private int maxRetries = 5;
    private int maxIterations = 50;
    private boolean requireManualReview = true;

    private String decisionTemplateId;  // 可选，指定使用的模板
    private String reviewTemplateId;

    // 从 JSON 解析
    public static SmartDecomposeConfig fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return new SmartDecomposeConfig();
        }
        return objectMapper.readValue(json, SmartDecomposeConfig.class);
    }
}
```

### 3.2 执行结果

```java
/**
 * 最终执行结果
 */
public class DecomposeExecutionResult {

    private String status;
    private int totalIterations;
    private int completedTaskCount;
    private int failedTaskCount;

    private List<TaskSummary> tasks;

    public static class TaskSummary {
        private String id;
        private String description;
        private String status;
        private String result;
    }
}
```

---

## 四、Repository 接口

### 4.1 PromptTemplateRepository

```java
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, String> {

    List<PromptTemplate> findByType(String type);

    Optional<PromptTemplate> findByTypeAndIsDefaultTrue(String type);

    @Modifying
    @Query("UPDATE PromptTemplate t SET t.isDefault = false WHERE t.type = :type")
    void clearDefaultByType(@Param("type") String type);
}
```

### 4.2 DecomposeExecutionStateRepository

```java
public interface DecomposeExecutionStateRepository extends JpaRepository<DecomposeExecutionState, String> {

    Optional<DecomposeExecutionState> findByExecutionId(String executionId);

    @Query("SELECT s FROM DecomposeExecutionState s WHERE s.status = 'WAITING_MANUAL_REVIEW'")
    List<DecomposeExecutionState> findWaitingForManualReview();
}
```

### 4.3 DecisionHistoryRepository

```java
public interface DecisionHistoryRepository extends JpaRepository<DecisionHistory, String> {

    List<DecisionHistory> findByExecutionIdOrderByIteration(String executionId);

    @Query("SELECT h FROM DecisionHistory h WHERE h.executionId = :executionId ORDER BY h.iteration DESC LIMIT 1")
    Optional<DecisionHistory> findLatestByExecutionId(@Param("executionId") String executionId);
}
```

### 4.4 ManualReviewRepository

```java
public interface ManualReviewRepository extends JpaRepository<ManualReviewRecord, String> {

    List<ManualReviewRecord> findByStatus(ManualReviewStatus status);

    Optional<ManualReviewRecord> findByExecutionIdAndTaskId(String executionId, String taskId);
}
```

---

## 五、状态序列化

### 5.1 任务队列序列化

```java
public class TaskQueueSerializer {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String serialize(Queue<SubTask> queue) {
        return mapper.writeValueAsString(new ArrayList<>(queue));
    }

    public static Queue<SubTask> deserialize(String json) {
        if (json == null || json.isEmpty()) {
            return new LinkedList<>();
        }
        List<SubTask> list = mapper.readValue(json,
            new TypeReference<List<SubTask>>() {});
        return new LinkedList<>(list);
    }
}
```

### 5.2 上下文持久化

```java
@Component
public class DecomposeStatePersister {

    private final DecomposeExecutionStateRepository stateRepository;

    public void save(DecomposeContext context) {
        DecomposeExecutionState state = new DecomposeExecutionState();
        state.setId(UUID.randomUUID().toString());
        state.setExecutionId(context.getExecutionId());
        state.setWorkflowId(context.getWorkflowId());
        state.setNodeId(context.getNodeId());
        state.setStatus(context.getStatus().name());
        state.setProjectPath(context.getProjectPath());
        state.setCurrentTaskId(context.getCurrentTask() != null ? context.getCurrentTask().getId() : null);
        state.setTaskQueue(TaskQueueSerializer.serialize(context.getTaskQueue()));
        state.setCompletedTasks(TaskQueueSerializer.serialize(new LinkedList<>(context.getCompletedTasks())));
        state.setFailedTasks(TaskQueueSerializer.serialize(new LinkedList<>(context.getFailedTasks())));
        state.setIterationCount(context.getIterationCount());
        state.setMaxIterations(context.getMaxIterations());
        state.setMaxRetries(context.getMaxRetries());
        state.setRequireManualReview(context.isRequireManualReview());
        state.setUpdatedAt(LocalDateTime.now());

        stateRepository.save(state);
    }

    public DecomposeContext load(String executionId) {
        DecomposeExecutionState state = stateRepository.findByExecutionId(executionId)
            .orElseThrow(() -> new IllegalStateException("执行状态不存在: " + executionId));

        DecomposeContext context = new DecomposeContext();
        context.setExecutionId(state.getExecutionId());
        context.setWorkflowId(state.getWorkflowId());
        context.setNodeId(state.getNodeId());
        context.setProjectPath(state.getProjectPath());
        context.setStatus(DecomposeContext.DecomposeStatus.valueOf(state.getStatus()));
        context.setTaskQueue(TaskQueueSerializer.deserialize(state.getTaskQueue()));
        context.setCompletedTasks(TaskQueueSerializer.deserialize(state.getCompletedTasks()));
        context.setFailedTasks(TaskQueueSerializer.deserialize(state.getFailedTasks()));
        context.setIterationCount(state.getIterationCount());
        context.setMaxIterations(state.getMaxIterations());
        context.setMaxRetries(state.getMaxRetries());
        context.setRequireManualReview(state.isRequireManualReview());

        return context;
    }
}
```