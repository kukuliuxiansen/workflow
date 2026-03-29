#!/usr/bin/env python3
"""
Workflows 项目历史功能测试脚本 v4
测试重点：
1. 暂停/恢复功能
2. 历史记录显示
3. UI/功能问题检查
"""
from playwright.sync_api import sync_playwright
import time
import os

TEST_URL = "http://localhost:3001"
SCREENSHOT_DIR = "/tmp/workflow_test_screenshots_v4"

def ensure_dir(path):
    if not os.path.exists(path):
        os.makedirs(path)

def save_screenshot(page, name):
    path = f"{SCREENSHOT_DIR}/{name}.png"
    page.screenshot(path=path)
    print(f"  截图: {name}")
    return path

def test_workflows_history():
    """主测试函数"""
    ensure_dir(SCREENSHOT_DIR)
    issues = []
    test_timestamp = int(time.time())

    print("\n" + "="*60)
    print("Workflows 历史功能测试 v4")
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

        # 处理对话框
        def handle_dialog(dialog):
            print(f"  对话框: {dialog.message[:50]}")
            dialog.accept()

        page.on('dialog', handle_dialog)

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

            page.locator("button:has-text('历史')").click()
            time.sleep(1)
            save_screenshot(page, "02_history_panel")

            # 检查历史列表
            history_items = page.locator("#historyList .history-item")
            history_count = history_items.count()
            print(f"  历史记录数: {history_count}")

            if history_count == 0:
                issues.append("历史记录列表为空")

            # 检查历史面板样式
            history_panel = page.locator("#historyPanel")
            panel_classes = history_panel.get_attribute("class")
            print(f"  历史面板类: {panel_classes}")

            if "show" not in panel_classes:
                issues.append("历史面板未正确显示(show类)")

            # ========================================
            # 步骤3: 测试历史记录详情
            # ========================================
            print("\n[步骤3] 测试历史记录详情...")

            if history_count > 0:
                # 点击第一条记录
                first_item = history_items.first
                item_title = first_item.locator(".history-item-title").text_content()
                item_status = first_item.locator(".history-item-status").text_content()
                print(f"  第一条: {item_title} ({item_status})")
                first_item.click()
                time.sleep(1)
                save_screenshot(page, "03_history_detail")

                # 检查详情内容
                detail = page.locator("#historyDetailContent")
                detail_text = detail.text_content()
                print(f"  详情内容: {detail_text[:100]}...")

                # 检查基本信息
                info_items = detail.locator("p")
                print(f"  信息项数: {info_items.count()}")

                # 检查操作按钮
                action_btns = detail.locator(".log-action-btn")
                btn_count = action_btns.count()
                print(f"  操作按钮数: {btn_count}")

                for i in range(btn_count):
                    btn = action_btns.nth(i)
                    btn_text = btn.text_content()
                    print(f"    按钮[{i}]: {btn_text}")

                # ========================================
                # 步骤4: 测试暂停/恢复功能
                # ========================================
                print("\n[步骤4] 测试暂停/恢复功能...")

                # 获取当前状态
                status_el = detail.locator(".history-item-status")
                current_status = status_el.first.text_content() if status_el.count() > 0 else "未知"
                print(f"  当前状态: {current_status}")

                if "暂停" in current_status:
                    print("  发现暂停状态，测试恢复功能...")

                    # 查找继续按钮
                    resume_btns = detail.locator(".log-action-btn")
                    for i in range(resume_btns.count()):
                        btn = resume_btns.nth(i)
                        if "继续" in btn.text_content():
                            print("  点击继续按钮...")
                            btn.click()
                            time.sleep(2)  # 等待确认对话框和请求
                            save_screenshot(page, "04_after_resume")

                            # 检查状态是否变化
                            new_status = status_el.first.text_content() if status_el.count() > 0 else ""
                            print(f"  新状态: {new_status}")

                            # 检查工具栏按钮
                            btn_pause = page.locator("#btnPause")
                            btn_resume = page.locator("#btnResume")
                            print(f"  工具栏暂停按钮: {btn_pause.is_visible() if btn_pause.count() > 0 else '不存在'}")
                            print(f"  工具栏继续按钮: {btn_resume.is_visible() if btn_resume.count() > 0 else '不存在'}")
                            break

                elif "运行" in current_status or "running" in current_status.lower():
                    print("  发现运行状态，测试暂停功能...")

                    for i in range(resume_btns.count()):
                        btn = resume_btns.nth(i)
                        if "暂停" in btn.text_content():
                            print("  点击暂停按钮...")
                            btn.click()
                            time.sleep(2)
                            save_screenshot(page, "04_after_pause")

                            new_status = status_el.first.text_content() if status_el.count() > 0 else ""
                            print(f"  新状态: {new_status}")
                            break

                else:
                    print(f"  当前状态 '{current_status}' 不支持暂停/恢复测试")

            # ========================================
            # 步骤5: 测试历史记录切换
            # ========================================
            print("\n[步骤5] 测试历史记录切换...")

            if history_count > 1:
                # 点击第二条记录
                second_item = history_items.nth(1)
                second_title = second_item.locator(".history-item-title").text_content()
                print(f"  点击第二条: {second_title}")
                second_item.click()
                time.sleep(1)
                save_screenshot(page, "05_second_record")

                # 检查选中状态
                first_selected = "selected" in history_items.first.get_attribute("class")
                second_selected = "selected" in second_item.get_attribute("class")
                print(f"  第一条选中: {first_selected}, 第二条选中: {second_selected}")

                if first_selected:
                    issues.append("历史记录切换时，之前的选中状态未取消")

            # ========================================
            # 步骤6: 测试刷新功能
            # ========================================
            print("\n[步骤6] 测试刷新功能...")

            refresh_btn = page.locator("#historyList").locator("..").locator("button:has-text('🔄')")
            if refresh_btn.count() > 0:
                print("  点击刷新按钮...")
                refresh_btn.click()
                time.sleep(1)
                save_screenshot(page, "06_after_refresh")

                # 检查是否重新加载
                new_history_count = page.locator("#historyList .history-item").count()
                print(f"  刷新后历史记录数: {new_history_count}")

            # ========================================
            # 步骤7: 测试关闭历史面板
            # ========================================
            print("\n[步骤7] 测试关闭历史面板...")

            close_btn = page.locator("#historyPanel .modal-close")
            if close_btn.count() > 0:
                close_btn.click()
                time.sleep(0.5)
                save_screenshot(page, "07_panel_closed")

                # 检查面板是否关闭
                panel_classes = page.locator("#historyPanel").get_attribute("class")
                if "show" in panel_classes:
                    issues.append("关闭历史面板后，show类仍然存在")

            # ========================================
            # 步骤8: 检查工具栏执行按钮状态
            # ========================================
            print("\n[步骤8] 检查工具栏按钮状态...")

            btn_execute = page.locator("#btnExecute")
            btn_pause = page.locator("#btnPause")
            btn_resume = page.locator("#btnResume")

            print(f"  执行按钮可见: {btn_execute.is_visible() if btn_execute.count() > 0 else '不存在'}")
            print(f"  暂停按钮可见: {btn_pause.is_visible() if btn_pause.count() > 0 else '不存在'}")
            print(f"  继续按钮可见: {btn_resume.is_visible() if btn_resume.count() > 0 else '不存在'}")

            # ========================================
            # 步骤9: 检查日志面板
            # ========================================
            print("\n[步骤9] 检查日志面板...")

            log_panel = page.locator("#logPanel")
            if log_panel.is_visible():
                print("  日志面板可见")

                # 检查各个标签
                tabs = ["执行日志", "Agent交互", "API日志", "操作日志"]
                for tab_name in tabs:
                    tab = page.locator(f".log-main-tab:has-text('{tab_name}')")
                    if tab.count() > 0:
                        tab.click()
                        time.sleep(0.3)
                        badge = tab.locator(".badge")
                        if badge.count() > 0:
                            count = badge.text_content()
                            print(f"  {tab_name}: {count}")

                save_screenshot(page, "08_logs")

            # ========================================
            # 步骤10: 检查控制台错误
            # ========================================
            print("\n[步骤10] 检查控制台...")

            errors = [m for m in console_messages if m['type'] == 'error']
            warnings = [m for m in console_messages if m['type'] == 'warning']

            print(f"  控制台错误: {len(errors)}")
            print(f"  控制台警告: {len(warnings)}")

            for err in errors[:5]:
                err_text = err['text']
                if "favicon" not in err_text.lower() and "404" not in err_text:
                    print(f"    错误: {err_text[:80]}")
                    issues.append(f"控制台错误: {err_text[:80]}")

            for warn in warnings[:3]:
                print(f"    警告: {warn['text'][:80]}")

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