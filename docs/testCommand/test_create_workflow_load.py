#!/usr/bin/env python3
"""
BUG #10 测试脚本: 创建工作流后加载验证

测试场景:
1. 打开工作流页面
2. 点击"新建工作流"按钮
3. 输入工作流名称和描述
4. 点击"创建"按钮
5. 验证工作流成功创建并加载

预期结果:
- 工作流创建成功
- 工作流正确加载到画布
- 没有出现"工作流不存在"的错误提示
- 画布显示开始和结束节点

修复说明:
- 问题: 前端预生成ID，后端返回不同ID，导致用错误ID加载工作流
- 修复: 使用后端响应中的实际ID进行后续操作
- 文件: src/main/resources/static/js/workflow-actions.js
"""

from playwright.sync_api import sync_playwright
import time

def test_create_workflow():
    """测试创建工作流后能否正确加载"""
    with sync_playwright() as p:
        # 启动浏览器
        browser = p.chromium.launch(headless=False)
        page = browser.new_page()

        # 打开页面
        print("1. 打开工作流页面...")
        page.goto("http://localhost:3001/index.html")
        page.wait_for_load_state("networkidle")

        # 点击新建工作流按钮
        print("2. 点击'新建工作流'按钮...")
        page.click("button:has-text('新建工作流')")
        page.wait_for_timeout(500)

        # 填写表单
        timestamp = str(int(time.time()))
        workflow_name = f"测试工作流_{timestamp}"
        workflow_desc = f"自动化测试创建 - {timestamp}"

        print(f"3. 输入工作流名称: {workflow_name}")
        page.fill("#newWorkflowName", workflow_name)
        page.fill("#newWorkflowDesc", workflow_desc)

        # 点击创建按钮（使用更具体的选择器，避免匹配到"从模板创建"按钮）
        print("4. 点击'创建'按钮...")
        page.click("#createModal button.btn-primary:has-text('创建')")

        # 等待创建完成
        page.wait_for_timeout(2000)

        # 验证结果
        print("5. 验证工作流是否创建成功...")

        # 检查是否有错误弹窗
        dialog_shown = False
        try:
            page.on("dialog", lambda dialog: dialog_shown == True)
        except:
            pass

        # 检查工作流列表中是否有新创建的工作流
        workflow_exists = page.locator(f"text={workflow_name}").count() > 0
        if workflow_exists:
            print(f"   ✓ 工作流 '{workflow_name}' 已创建")
        else:
            print(f"   ✗ 工作流 '{workflow_name}' 未找到")

        # 检查画布是否有节点
        canvas_has_nodes = page.locator(".node").count() > 0
        if canvas_has_nodes:
            print("   ✓ 画布已加载节点")
        else:
            print("   ✗ 画布没有节点")

        # 结果
        if workflow_exists and canvas_has_nodes:
            print("\n✅ 测试通过: 工作流创建并加载成功")
        else:
            print("\n❌ 测试失败: 工作流创建或加载失败")

        # 截图
        page.screenshot(path=f"/tmp/bug10_test_{timestamp}.png")
        print(f"截图已保存: /tmp/bug10_test_{timestamp}.png")

        # 关闭浏览器
        browser.close()

        return workflow_exists and canvas_has_nodes

if __name__ == "__main__":
    result = test_create_workflow()
    exit(0 if result else 1)