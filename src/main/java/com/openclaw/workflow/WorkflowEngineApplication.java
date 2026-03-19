package com.openclaw.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Workflow Engine 主应用
 *
 * 功能：
 * - 工作流管理与编辑
 * - 可视化节点编辑器
 * - 执行控制（执行、暂停、继续、停止）
 * - OpenClaw集成
 * - 飞书通知
 */
@SpringBootApplication
public class WorkflowEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowEngineApplication.class, args);
        System.out.println("========================================");
        System.out.println("  Workflow Engine Started!");
        System.out.println("  http://localhost:3001");
        System.out.println("========================================");
    }
}