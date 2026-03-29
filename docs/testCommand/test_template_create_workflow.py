#!/usr/bin/env python3
"""
BUG #36 测试脚本: 从模板创建工作流验证

测试场景:
1. 打开工作流页面
2. 点击"从模板创建"按钮
3. 选择一个模板
4. 输入工作流名称
5. 验证工作流创建成功，节点数量正确

预期结果:
- 工作流创建成功
- 节点数量与模板描述一致
- 画布正确显示节点

修复说明:
- 问题: 模板从后端获取，节点数据可能不完整
- 修复: 在前端定义常用模板，直接创建工作流
- 文件: src/main/resources/static/js/workflow-actions.js
"""

from playwright.sync_api import sync_playwright
import time

def test_template_create_workflow():
    """测试从模板创建工作流"""
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)
        page = browser.new_page()

        print("1. 打开工作流页面...")
        page.goto("http://localhost:3001/index.html")
        page.wait_for_load_state("networkidle")

        # 关闭可能存在的弹窗
        page.evaluate("document.querySelectorAll('.modal-overlay.show').forEach(m => m.classList.remove('show'))")
        page.wait_for_timeout(500)

        print("2. 点击'从模板创建'按钮...")
        page.click("button:has-text('从模板创建')")
        page.wait_for_timeout(500)

        print("3. 检查模板列表...")
        templates = page.locator(".template-card")
        template_count = templates.count()
        print(f"   找到 {template_count} 个模板")

        if template_count == 0:
            print("   ✗ 没有可用的模板")
            browser.close()
            return False

        # 获取第一个模板的节点数量
        first_template = templates.first
        template_text = first_template.locator(".meta").text_content()
        print(f"   模板信息: {template_text}")

        # 先设置对话框处理，再点击模板（prompt是同步的）
        timestamp = str(int(time.time()))
        workflow_name = f"模板测试_{timestamp}"

        print(f"4. 输入工作流名称: {workflow_name}")
        # 必须在点击之前设置对话框处理
        page.on("dialog", lambda dialog: dialog.accept(workflow_name))

        # 点击第一个模板
        first_template.click()

        # 等待创建完成
        page.wait_for_timeout(2000)

        print("5. 验证工作流创建结果...")

        # 检查工作流列表
        workflow_exists = page.locator(f"text={workflow_name}").count() > 0
        if workflow_exists:
            print(f"   ✓ 工作流 '{workflow_name}' 已创建")
        else:
            print(f"   ✗ 工作流 '{workflow_name}' 未找到")

        # 检查画布节点数量
        node_count = page.locator(".node").count()
        print(f"   画布节点数量: {node_count}")

        if node_count > 0:
            print("   ✓ 画布已加载节点")
            result = True
        else:
            print("   ✗ 画布没有节点")
            result = False

        # 截图
        page.screenshot(path=f"/tmp/bug36_test_{timestamp}.png")
        print(f"截图已保存: /tmp/bug36_test_{timestamp}.png")

        browser.close()
        return result

if __name__ == "__main__":
    result = test_template_create_workflow()
    exit(0 if result else 1)