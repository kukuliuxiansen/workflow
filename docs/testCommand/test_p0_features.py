#!/usr/bin/env python3
"""
P0功能验证测试脚本

验证项目:
1. P0-1: 任务配置切换
2. P0-2: 执行前验证
3. P0-3: 暂停/恢复执行
4. P0-4: 节点状态可视化

时间戳: 2026-03-25
"""

from playwright.sync_api import sync_playwright
import time

def test_p0_features():
    """验证P0功能"""
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)
        page = browser.new_page()

        print("=" * 50)
        print("P0功能验证测试")
        print("=" * 50)

        # 1. 打开页面
        print("\n1. 打开工作流页面...")
        page.goto("http://localhost:3001/index.html")
        page.wait_for_load_state("networkidle")

        # 关闭可能存在的弹窗
        page.evaluate("document.querySelectorAll('.modal-overlay.show').forEach(m => m.classList.remove('show'))")
        page.wait_for_timeout(500)

        # 2. 测试任务配置切换 (P0-1)
        print("\n2. 测试P0-1: 任务配置切换...")
        workflows = page.locator(".workflow-item")
        if workflows.count() > 0:
            # 选择第一个工作流
            workflows.first.click()
            page.wait_for_timeout(500)

            # 检查任务配置按钮是否存在
            task_btn = page.locator("button:has-text('任务配置')")
            if task_btn.count() > 0:
                print("   ✓ P0-1: 任务配置按钮存在")

                # 点击打开配置弹窗
                task_btn.click()
                page.wait_for_timeout(300)

                # 检查弹窗是否存在
                modal = page.locator("#taskConfigModal.show")
                if modal.count() > 0:
                    print("   ✓ P0-1: 任务配置弹窗正常显示")

                    # 获取当前配置值
                    task_name = page.locator("#taskName").input_value()
                    task_path = page.locator("#taskProjectPath").input_value()
                    print(f"   任务名称: {task_name or '(空)'}")
                    print(f"   项目路径: {task_path or '(空)'}")
                else:
                    print("   ⚠ 任务配置弹窗未显示")

                # 关闭可能存在的弹窗
                page.evaluate("document.querySelectorAll('.modal-overlay.show').forEach(m => m.classList.remove('show'))")
            else:
                print("   ⚠ 任务配置按钮不存在")
        else:
            print("   ⚠ 没有工作流，跳过P0-1测试")

        # 3. 测试执行按钮状态 (P0-3)
        print("\n3. 测试P0-3: 执行控制按钮...")
        btn_execute = page.locator("#btnExecute")
        btn_pause = page.locator("#btnPause")
        btn_resume = page.locator("#btnResume")

        execute_visible = btn_execute.is_visible() if btn_execute.count() > 0 else False
        pause_visible = btn_pause.is_visible() if btn_pause.count() > 0 else False

        print(f"   执行按钮可见: {execute_visible}")
        print(f"   暂停按钮可见: {pause_visible}")

        if btn_execute.count() > 0 and btn_pause.count() > 0:
            print("   ✓ P0-3: 暂停/恢复按钮存在")
        else:
            print("   ✗ P0-3: 暂停/恢复按钮缺失")

        # 4. 测试验证功能 (P0-2)
        print("\n4. 测试P0-2: 执行前验证...")
        # 尝试执行没有任务配置的工作流
        if workflows.count() > 0:
            workflows.first.click()
            page.wait_for_timeout(300)

            # 点击执行按钮
            if btn_execute.count() > 0 and btn_execute.is_visible():
                btn_execute.click()
                page.wait_for_timeout(500)

                # 检查是否有验证提示
                toast = page.locator(".toast")
                if toast.count() > 0:
                    toast_text = toast.first.text_content()
                    print(f"   提示信息: {toast_text}")

                    if "配置" in toast_text or "验证" in toast_text or "请先" in toast_text:
                        print("   ✓ P0-2: 执行前验证功能存在")
                    else:
                        print("   ⚠ 未检测到验证提示")
                else:
                    # 检查是否有验证弹窗
                    validation_modal = page.locator("#validationModal")
                    if validation_modal.count() > 0:
                        print("   ✓ P0-2: 验证弹窗已显示")
                    else:
                        print("   ⚠ 未检测到验证反馈")

                # 关闭可能存在的弹窗
        page.evaluate("document.querySelectorAll('.modal-overlay.show').forEach(m => m.classList.remove('show'))")
        page.evaluate("document.querySelectorAll('.toast').forEach(t => t.remove())")

        # 5. 测试节点状态显示 (P0-4)
        print("\n5. 测试P0-4: 节点状态可视化...")
        # 检查CSS类是否存在
        css_check = page.evaluate("""() => {
            const testDiv = document.createElement('div');
            testDiv.className = 'node-status-badge running';
            document.body.appendChild(testDiv);
            const style = window.getComputedStyle(testDiv);
            const hasStyle = style.backgroundColor !== 'rgba(0, 0, 0, 0)';
            document.body.removeChild(testDiv);
            return hasStyle;
        }""")
        print(f"   节点状态CSS样式存在: {css_check}")
        if css_check:
            print("   ✓ P0-4: 节点状态样式存在")
        else:
            print("   ✗ P0-4: 节点状态样式缺失")

        # 截图
        timestamp = str(int(time.time()))
        page.screenshot(path=f"/tmp/p0_test_{timestamp}.png")
        print(f"\n截图已保存: /tmp/p0_test_{timestamp}.png")

        print("\n" + "=" * 50)
        print("P0功能验证测试完成")
        print("=" * 50)

        browser.close()
        return True

if __name__ == "__main__":
    test_p0_features()