#!/usr/bin/env python3
"""
Workflows 项目历史功能测试脚本 v2
改进：处理验证模态框，选择完整配置的工作流
"""
from playwright.sync_api import sync_playwright
import time
import json
import os

TEST_URL = "http://localhost:3001"
SCREENSHOT_DIR = "/tmp/workflow_test_screenshots_v2"

def ensure_dir(path):
    if not os.path.exists(path):
        os.makedirs(path)

def save_screenshot(page, name):
    path = f"{SCREENSHOT_DIR}/{name}.png"
    page.screenshot(path=path)
    print(f"  截图保存: {path}")
    return path

def close_any_modal(page):
    """关闭任何可见的模态框"""
    # 尝试关闭动态创建的 validationModal
    try:
        validation_modal = page.locator("#validationModal")
        if validation_modal.count() > 0 and validation_modal.is_visible():
            # 点击关闭按钮或取消按钮
            close_btn = validation_modal.locator("button:has-text('关闭'), button:has-text('取消'), .close-btn, .modal-close")
            if close_btn.count() > 0:
                close_btn.first.click()
                time.sleep(0.3)
                print("  关闭了验证模态框")
    except Exception as e:
        print(f"  关闭模态框异常: {e}")

    # 尝试关闭其他模态框
    try:
        modals = page.locator(".modal-overlay.show")
        for i in range(modals.count()):
            modal = modals.nth(i)
            if modal.is_visible():
                close_btn = modal.locator("button:has-text('关闭'), button:has-text('取消'), .modal-close, .close-btn")
                if close_btn.count() > 0:
                    close_btn.first.click()
                    time.sleep(0.3)
    except:
        pass

def test_workflows_history():
    """主测试函数"""
    ensure_dir(SCREENSHOT_DIR)

    issues = []  # 记录发现的问题
    test_timestamp = int(time.time())

    print("\n" + "="*60)
    print("Workflows 历史功能测试 v2")
    print(f"测试时间: {test_timestamp}")
    print("="*60)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)
        context = browser.new_context()
        page = context.new_page()

        # 监听控制台消息
        console_messages = []
        page.on('console', lambda msg: console_messages.append({
            'type': msg.type,
            'text': msg.text
        }))

        # 监听页面错误
        page_errors = []
        page.on('pageerror', lambda err: page_errors.append(str(err)))

        try:
            # ========================================
            # 步骤1: 打开页面
            # ========================================
            print("\n[步骤1] 打开页面...")
            page.goto(TEST_URL, timeout=10000)
            time.sleep(2)
            save_screenshot(page, "01_initial_page")

            # ========================================
            # 步骤2: 查看历史记录
            # ========================================
            print("\n[步骤2] 打开执行历史面板...")

            history_btn = page.locator("button:has-text('历史')")
            if history_btn.count() > 0:
                history_btn.click()
                time.sleep(1)
                save_screenshot(page, "02_history_panel")

                # 检查历史列表
                history_list = page.locator("#historyList .history-item")
                history_count = history_list.count()
                print(f"  发现 {history_count} 条历史记录")

                # 检查是否有"暂无执行记录"
                empty_state = page.locator("#historyList .empty-state")
                if empty_state.count() > 0:
                    print("  提示: 暂无执行记录")

                # 检查加载状态
                loading = page.locator("#historyList .loading")
                if loading.count() > 0 and loading.is_visible():
                    print("  警告: 历史列表仍在加载中")
            else:
                issues.append("未找到历史按钮")

            # 关闭历史面板
            close_btn = page.locator("#historyPanel .modal-close")
            if close_btn.count() > 0 and close_btn.is_visible():
                close_btn.click()
                time.sleep(0.5)

            # ========================================
            # 步骤3: 查找工作流并选择一个完整的
            # ========================================
            print("\n[步骤3] 选择工作流...")

            workflow_items = page.locator("#workflowList .workflow-item")
            workflow_count = workflow_items.count()
            print(f"  发现 {workflow_count} 个工作流")

            # 尝试找一个有完整节点的工作流
            selected_workflow = None
            for i in range(min(workflow_count, 10)):  # 最多检查前10个
                item = workflow_items.nth(i)
                item.click()
                time.sleep(0.5)

                # 检查节点数量
                nodes = page.locator("#canvasContent .node")
                node_count = nodes.count()
                workflow_name = item.locator(".workflow-name").text_content() if item.locator(".workflow-name").count() > 0 else f"工作流{i+1}"
                print(f"    [{i+1}] {workflow_name}: {node_count} 个节点")

                if node_count >= 2:  # 至少有2个节点
                    selected_workflow = workflow_name
                    print(f"  选择: {selected_workflow}")
                    break

            if not selected_workflow and workflow_count > 0:
                workflow_items.first.click()
                selected_workflow = "第一个工作流"
                print(f"  选择默认: {selected_workflow}")

            save_screenshot(page, "03_selected_workflow")

            # ========================================
            # 步骤4: 检查按钮状态
            # ========================================
            print("\n[步骤4] 检查执行按钮状态...")

            btn_execute = page.locator("#btnExecute")
            btn_pause = page.locator("#btnPause")
            btn_resume = page.locator("#btnResume")

            print(f"  执行按钮: {btn_execute.is_visible() if btn_execute.count() > 0 else '不存在'}")
            print(f"  暂停按钮: {btn_pause.is_visible() if btn_pause.count() > 0 else '不存在'}")
            print(f"  继续按钮: {btn_resume.is_visible() if btn_resume.count() > 0 else '不存在'}")

            # ========================================
            # 步骤5: 配置任务
            # ========================================
            print("\n[步骤5] 配置任务...")

            # 打开任务配置
            task_config_btn = page.locator("button:has-text('任务配置')")
            if task_config_btn.count() > 0:
                task_config_btn.click()
                time.sleep(0.5)
                save_screenshot(page, "04_task_config_modal")

                # 填写任务名称
                task_name_input = page.locator("#taskName")
                if task_name_input.count() > 0:
                    task_name_input.fill(f"测试任务_{test_timestamp}")
                    print("  已填写任务名称")

                # 填写任务描述
                task_desc_input = page.locator("#taskDescription")
                if task_desc_input.count() > 0:
                    task_desc_input.fill("这是一个自动化测试任务，用于验证历史功能")
                    print("  已填写任务描述")

                # 填写项目路径
                task_path_input = page.locator("#taskProjectPath")
                if task_path_input.count() > 0:
                    task_path_input.fill("/tmp/test_project")
                    print("  已填写项目路径")

                # 点击确认
                confirm_btn = page.locator("#taskConfigModal .btn-primary:has-text('确认配置')")
                if confirm_btn.count() > 0:
                    confirm_btn.click()
                    time.sleep(0.5)
                    print("  任务配置已保存")

            save_screenshot(page, "05_before_execute")

            # ========================================
            # 步骤6: 执行工作流
            # ========================================
            print("\n[步骤6] 尝试执行工作流...")

            if btn_execute.is_visible():
                btn_execute.click()
                print("  已点击执行按钮")
                time.sleep(1)

                # 检查是否弹出验证模态框
                validation_modal = page.locator("#validationModal")
                if validation_modal.count() > 0 and validation_modal.is_visible():
                    print("  发现验证模态框")
                    save_screenshot(page, "06_validation_modal")

                    # 检查模态框内容
                    modal_body = validation_modal.locator(".modal-body")
                    if modal_body.count() > 0:
                        content = modal_body.text_content()
                        print(f"  模态框内容: {content[:200]}...")

                    # 检查按钮类型
                    ignore_btn = validation_modal.locator("button:has-text('忽略警告')")
                    close_btn_modal = validation_modal.locator("button:has-text('关闭'), button:has-text('取消')")

                    if ignore_btn.count() > 0:
                        print("  发现'忽略警告'按钮 - 表示只有警告")
                        issues.append("工作流验证有警告，需要用户确认")
                        ignore_btn.click()
                        time.sleep(2)
                    elif close_btn_modal.count() > 0:
                        print("  只有'关闭'按钮 - 表示有错误")
                        issues.append("工作流验证有错误，无法执行")

                        # 查看错误详情
                        error_items = validation_modal.locator(".error-item, .validation-error")
                        for j in range(min(error_items.count(), 3)):
                            print(f"    错误: {error_items.nth(j).text_content()}")

                        close_btn_modal.first.click()
                        time.sleep(0.5)
                else:
                    # 没有验证模态框，检查确认对话框
                    save_screenshot(page, "06_executing")

                # 检查按钮状态变化
                time.sleep(1)
                execute_visible_after = btn_execute.is_visible() if btn_execute.count() > 0 else False
                pause_visible_after = btn_pause.is_visible() if btn_pause.count() > 0 else False

                print(f"  执行后 - 执行按钮可见: {execute_visible_after}")
                print(f"  执行后 - 暂停按钮可见: {pause_visible_after}")

                if execute_visible_after and pause_visible_after:
                    issues.append("执行后按钮状态异常: 执行和暂停按钮同时可见")
                elif execute_visible_after:
                    # 可能是验证失败或用户取消了确认
                    print("  执行按钮仍然可见 - 可能验证失败或用户取消")

            # ========================================
            # 步骤7: 检查日志面板
            # ========================================
            print("\n[步骤7] 检查日志面板...")

            log_panel = page.locator("#logPanel")
            if log_panel.is_visible():
                print("  日志面板可见")

                # 检查执行日志内容
                exec_log_content = page.locator("#execLogContent")
                if exec_log_content.count() > 0:
                    log_items = exec_log_content.locator(".log-item")
                    print(f"  执行日志条数: {log_items.count()}")

                # 检查日志数量徽章
                exec_log_count = page.locator("#execLogCount")
                if exec_log_count.count() > 0:
                    count_text = exec_log_count.text_content()
                    print(f"  执行日志数量徽章: {count_text}")

                # 切换到 Agent交互 标签
                agent_tab = page.locator(".log-main-tab:has-text('Agent交互')")
                if agent_tab.count() > 0:
                    agent_tab.click()
                    time.sleep(0.3)
                    agent_log_count = page.locator("#agentLogCount")
                    if agent_log_count.count() > 0:
                        print(f"  Agent日志数量: {agent_log_count.text_content()}")
            else:
                issues.append("日志面板不可见")

            save_screenshot(page, "07_logs")

            # ========================================
            # 步骤8: 再次检查历史记录
            # ========================================
            print("\n[步骤8] 再次检查历史记录...")

            # 先关闭任何模态框
            close_any_modal(page)
            time.sleep(0.3)

            history_btn = page.locator("button:has-text('历史')")
            if history_btn.count() > 0 and history_btn.is_visible():
                history_btn.click()
                time.sleep(1)
                save_screenshot(page, "08_history_after_execute")

                history_list = page.locator("#historyList .history-item")
                new_count = history_list.count()
                print(f"  现在有 {new_count} 条历史记录")

                if new_count > 0:
                    # 点击第一条记录查看详情
                    first_history = history_list.first
                    history_name = first_history.locator(".history-item-title").text_content() if first_history.locator(".history-item-title").count() > 0 else "未命名"
                    print(f"  第一条记录: {history_name}")
                    first_history.click()
                    time.sleep(1)
                    save_screenshot(page, "09_history_detail")

                    # 检查详情内容
                    detail_content = page.locator("#historyDetailContent")
                    if detail_content.count() > 0:
                        # 检查是否有基本信息
                        info_items = detail_content.locator("p")
                        print(f"  详情信息项数: {info_items.count()}")

                        # 检查操作按钮
                        action_btns = detail_content.locator(".log-action-btn")
                        action_count = action_btns.count()
                        print(f"  操作按钮数量: {action_count}")

                        if action_count > 0:
                            # 列出所有按钮
                            for k in range(action_count):
                                btn_text = action_btns.nth(k).text_content()
                                print(f"    按钮: {btn_text}")
            else:
                print("  历史按钮不可见")

            # ========================================
            # 步骤9: 测试历史记录详情功能
            # ========================================
            print("\n[步骤9] 测试历史记录详情功能...")

            # 检查是否有正在运行的记录（可以测试暂停）
            running_status = page.locator("#historyDetailContent .history-item-status.running")
            paused_status = page.locator("#historyDetailContent .history-item-status.paused")
            completed_status = page.locator("#historyDetailContent .history-item-status.completed")

            print(f"  运行中记录: {running_status.count()}")
            print(f"  已暂停记录: {paused_status.count()}")
            print(f"  已完成记录: {completed_status.count()}")

            # 如果有暂停的记录，测试恢复
            if paused_status.count() > 0:
                print("  发现暂停的记录")
                resume_btn = page.locator("#historyDetailContent .log-action-btn:has-text('继续')")
                if resume_btn.count() > 0:
                    print("  找到'继续'按钮")
                    # 不实际点击，避免改变状态

            # 如果有运行中的记录，测试暂停
            if running_status.count() > 0:
                print("  发现运行中的记录")
                pause_btn = page.locator("#historyDetailContent .log-action-btn:has-text('暂停')")
                if pause_btn.count() > 0:
                    print("  找到'暂停'按钮")

            # ========================================
            # 检查控制台错误
            # ========================================
            print("\n[检查控制台消息]")

            errors = [m for m in console_messages if m['type'] == 'error']
            warnings = [m for m in console_messages if m['type'] == 'warning']

            print(f"  控制台错误: {len(errors)}")
            print(f"  控制台警告: {len(warnings)}")

            if errors:
                for err in errors[:5]:
                    err_text = err['text'][:100]
                    print(f"    错误: {err_text}")
                    # 过滤掉一些已知的、不重要的问题
                    if "favicon" not in err_text.lower() and "404" not in err_text:
                        issues.append(f"控制台错误: {err_text}")

            if page_errors:
                for err in page_errors[:3]:
                    print(f"    页面错误: {err[:100]}")
                    issues.append(f"页面错误: {err[:100]}")

            # ========================================
            # 等待用户查看
            # ========================================
            print("\n测试完成，浏览器保持打开状态...")
            print("按 Ctrl+C 关闭浏览器")

            try:
                time.sleep(5)
            except KeyboardInterrupt:
                pass

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
        print("\n未发现明显问题")

    return issues

if __name__ == "__main__":
    issues = test_workflows_history()
    exit(0 if not issues else 1)