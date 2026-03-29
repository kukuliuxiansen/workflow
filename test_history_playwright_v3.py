#!/usr/bin/env python3
"""
Workflows 项目历史功能测试脚本 v3
测试重点：
1. 暂停/恢复功能
2. 实时更新
3. 历史记录显示
"""
from playwright.sync_api import sync_playwright
import time
import json
import os

TEST_URL = "http://localhost:3001"
SCREENSHOT_DIR = "/tmp/workflow_test_screenshots_v3"

def ensure_dir(path):
    if not os.path.exists(path):
        os.makedirs(path)

def save_screenshot(page, name):
    path = f"{SCREENSHOT_DIR}/{name}.png"
    page.screenshot(path=path)
    print(f"  截图: {name}")
    return path

def js_close_modal(page, modal_id="validationModal"):
    """使用 JavaScript 关闭模态框"""
    page.evaluate(f"""
        const modal = document.getElementById('{modal_id}');
        if (modal) {{
            modal.classList.remove('show');
            modal.remove();
        }}
    """)
    print(f"  JS关闭模态框: {modal_id}")

def test_workflows_history():
    """主测试函数"""
    ensure_dir(SCREENSHOT_DIR)
    issues = []
    test_timestamp = int(time.time())

    print("\n" + "="*60)
    print("Workflows 历史功能测试 v3")
    print(f"测试时间: {test_timestamp}")
    print("="*60)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)
        context = browser.new_context()
        page = context.new_page()

        console_messages = []
        page.on('console', lambda msg: console_messages.append({
            'type': msg.type,
            'text': msg.text
        }))

        page_errors = []
        page.on('pageerror', lambda err: page_errors.append(str(err)))

        try:
            # ========================================
            # 步骤1: 打开页面
            # ========================================
            print("\n[步骤1] 打开页面...")
            page.goto(TEST_URL, timeout=10000)
            time.sleep(2)
            save_screenshot(page, "01_initial")

            # ========================================
            # 步骤2: 测试历史面板
            # ========================================
            print("\n[步骤2] 测试历史面板...")

            # 点击历史按钮
            page.locator("button:has-text('历史')").click()
            time.sleep(1)
            save_screenshot(page, "02_history_panel")

            # 检查历史列表
            history_items = page.locator("#historyList .history-item")
            history_count = history_items.count()
            print(f"  历史记录数: {history_count}")

            # 检查历史面板是否正确显示
            history_panel = page.locator("#historyPanel")
            if not history_panel.is_visible():
                issues.append("历史面板未正确显示")

            # 关闭历史面板
            page.locator("#historyPanel .modal-close").click()
            time.sleep(0.5)

            # ========================================
            # 步骤3: 选择一个有完整配置的工作流
            # ========================================
            print("\n[步骤3] 选择工作流...")

            # 获取所有工作流
            workflow_items = page.locator("#workflowList .workflow-item")
            workflow_count = workflow_items.count()
            print(f"  工作流数: {workflow_count}")

            # 选择第一个工作流
            workflow_items.first.click()
            time.sleep(1)
            save_screenshot(page, "03_workflow_selected")

            # ========================================
            # 步骤4: 配置任务
            # ========================================
            print("\n[步骤4] 配置任务...")

            page.locator("button:has-text('任务配置')").click()
            time.sleep(0.5)

            page.locator("#taskName").fill(f"测试任务_{test_timestamp}")
            page.locator("#taskDescription").fill("自动化测试任务")
            page.locator("#taskProjectPath").fill("/tmp/test_project")

            page.locator("#taskConfigModal .btn-primary").click()
            time.sleep(0.5)
            print("  任务配置完成")

            # ========================================
            # 步骤5: 尝试执行工作流
            # ========================================
            print("\n[步骤5] 执行工作流...")

            page.locator("#btnExecute").click()
            time.sleep(1)

            # 检查是否有验证模态框
            validation_modal = page.locator("#validationModal")
            if validation_modal.count() > 0 and validation_modal.is_visible():
                print("  发现验证模态框")

                # 检查是否有忽略警告按钮
                ignore_btn = validation_modal.locator("button:has-text('忽略警告')")
                if ignore_btn.count() > 0:
                    print("  点击'忽略警告'继续执行")
                    # 使用 JS 点击
                    page.evaluate("""
                        const btn = document.querySelector('#validationModal button:has-text("忽略警告")');
                        if (btn) btn.click();
                    """)
                    time.sleep(2)
                else:
                    # 有错误，无法执行
                    modal_text = validation_modal.locator(".modal-body").text_content()
                    print(f"  验证错误: {modal_text[:200]}")
                    issues.append(f"工作流验证错误: {modal_text[:100]}")

                    # 使用 JS 关闭模态框
                    js_close_modal(page, "validationModal")
                    time.sleep(0.5)

            save_screenshot(page, "04_after_execute_attempt")

            # ========================================
            # 步骤6: 测试历史记录详情
            # ========================================
            print("\n[步骤6] 测试历史记录详情...")

            # 关闭任何剩余模态框
            page.evaluate("""
                document.querySelectorAll('.modal-overlay.show').forEach(m => m.classList.remove('show'));
            """)
            time.sleep(0.3)

            # 打开历史面板
            page.locator("button:has-text('历史')").click()
            time.sleep(1)
            save_screenshot(page, "05_history_list")

            # 点击第一条历史记录
            if history_items.count() > 0:
                history_items.first.click()
                time.sleep(1)
                save_screenshot(page, "06_history_detail")

                # 检查详情内容
                detail = page.locator("#historyDetailContent")

                # 获取状态
                status_el = detail.locator(".history-item-status")
                if status_el.count() > 0:
                    status = status_el.first.text_content()
                    status_class = status_el.first.get_attribute("class")
                    print(f"  记录状态: {status} (class: {status_class})")

                # 检查操作按钮
                action_btns = detail.locator(".log-action-btn")
                btn_count = action_btns.count()
                print(f"  操作按钮数: {btn_count}")

                for i in range(btn_count):
                    btn_text = action_btns.nth(i).text_content()
                    print(f"    - {btn_text}")

                # ========================================
                # 步骤7: 测试暂停/恢复功能
                # ========================================
                print("\n[步骤7] 测试暂停/恢复功能...")

                # 检查是否有暂停状态的记录
                paused_status = detail.locator(".history-item-status.paused")
                running_status = detail.locator(".history-item-status.running")

                if paused_status.count() > 0:
                    print("  发现暂停状态的记录")

                    # 查找继续按钮
                    resume_btn = detail.locator(".log-action-btn:has-text('继续')")
                    if resume_btn.count() > 0:
                        print("  测试恢复功能...")
                        # 点击继续按钮
                        page.evaluate("""
                            const btn = document.querySelector('#historyDetailContent .log-action-btn:has-text("继续")');
                            if (btn) btn.click();
                        """)

                        # 等待确认对话框
                        time.sleep(1)

                        # 处理确认对话框 - 使用 accept
                        page.on("dialog", lambda dialog: dialog.accept())
                        time.sleep(2)

                        save_screenshot(page, "07_after_resume")
                        print("  恢复操作已执行")

                elif running_status.count() > 0:
                    print("  发现运行状态的记录")

                    # 查找暂停按钮
                    pause_btn = detail.locator(".log-action-btn:has-text('暂停')")
                    if pause_btn.count() > 0:
                        print("  测试暂停功能...")
                        page.evaluate("""
                            const btn = document.querySelector('#historyDetailContent .log-action-btn:has-text("暂停")');
                            if (btn) btn.click();
                        """)
                        time.sleep(1)
                        page.on("dialog", lambda dialog: dialog.accept())
                        time.sleep(2)
                        save_screenshot(page, "07_after_pause")

                else:
                    print("  未发现暂停或运行状态的记录")

            # ========================================
            # 步骤8: 测试实时更新
            # ========================================
            print("\n[步骤8] 测试实时更新...")

            # 检查日志面板
            log_panel = page.locator("#logPanel")
            if log_panel.is_visible():
                # 切换到执行日志
                page.locator(".log-main-tab:has-text('执行日志')").click()
                time.sleep(0.3)

                exec_log_count = page.locator("#execLogCount").text_content()
                print(f"  执行日志数: {exec_log_count}")

                # 切换到 Agent交互
                page.locator(".log-main-tab:has-text('Agent交互')").click()
                time.sleep(0.3)

                agent_log_count = page.locator("#agentLogCount").text_content()
                print(f"  Agent日志数: {agent_log_count}")

            save_screenshot(page, "08_logs_check")

            # ========================================
            # 步骤9: 检查节点状态显示
            # ========================================
            print("\n[步骤9] 检查节点状态显示...")

            # 关闭历史面板
            close_btn = page.locator("#historyPanel .modal-close")
            if close_btn.is_visible():
                close_btn.click()
                time.sleep(0.5)

            # 检查画布上的节点状态
            nodes = page.locator("#canvasContent .node")
            node_count = nodes.count()
            print(f"  节点数: {node_count}")

            # 检查是否有节点显示状态
            for i in range(min(node_count, 5)):
                node = nodes.nth(i)
                node_classes = node.get_attribute("class")
                if "running" in node_classes or "completed" in node_classes or "failed" in node_classes:
                    print(f"  节点 {i+1} 有状态: {node_classes}")

            save_screenshot(page, "09_canvas_state")

            # ========================================
            # 步骤10: 检查控制台错误
            # ========================================
            print("\n[步骤10] 检查控制台...")

            errors = [m for m in console_messages if m['type'] == 'error']
            print(f"  控制台错误: {len(errors)}")

            for err in errors[:5]:
                err_text = err['text'][:100]
                if "favicon" not in err_text.lower():
                    print(f"    错误: {err_text}")
                    issues.append(f"控制台错误: {err_text}")

            # ========================================
            # 测试完成
            # ========================================
            print("\n测试完成!")
            time.sleep(3)

        except Exception as e:
            print(f"\n测试异常: {e}")
            save_screenshot(page, "error_state")
            issues.append(f"测试异常: {str(e)}")

        finally:
            browser.close()

    # ========================================
    # 输出测试报告
    # ========================================
    print("\n" + "="*60)
    print("测试报告")
    print("="*60)

    if issues:
        print(f"\n发现 {len(issues)} 个问题:")
        for i, issue in enumerate(issues, 1):
            print(f"  {i}. {issue}")
    else:
        print("\n未发现问题")

    return issues

if __name__ == "__main__":
    issues = test_workflows_history()
    exit(0 if not issues else 1)