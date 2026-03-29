#!/usr/bin/env python3
"""
BUG #32 测试脚本: AI生成工作流后加载验证

测试场景:
1. 打开工作流页面
2. 点击"✨ AI生成"按钮
3. 输入工作流描述
4. 点击"✨ 生成工作流"按钮
5. 等待AI生成完成
6. 验证工作流成功创建并加载

预期结果:
- AI生成请求成功
- 工作流创建成功
- 工作流正确加载到画布
- 没有出现"工作流不存在"的错误提示

修复说明:
- 问题: 同BUG #10，使用AI返回的ID而非后端保存后的实际ID
- 修复: 使用后端保存响应中的实际ID进行后续操作
- 文件: src/main/resources/static/js/task-config.js
"""

from playwright.sync_api import sync_playwright
import time

def test_ai_generate_workflow():
    """测试AI生成工作流后能否正确加载"""
    with sync_playwright() as p:
        # 启动浏览器
        browser = p.chromium.launch(headless=False)
        page = browser.new_page()

        # 打开页面
        print("1. 打开工作流页面...")
        page.goto("http://localhost:3001/index.html")
        page.wait_for_load_state("networkidle")

        # 关闭可能存在的弹窗
        try:
            page.evaluate("document.querySelectorAll('.modal-overlay.show').forEach(m => m.classList.remove('show'))")
        except:
            pass

        # 点击AI生成按钮
        print("2. 点击'✨ AI生成'按钮...")
        page.click("button:has-text('AI生成')")
        page.wait_for_timeout(500)

        # 填写描述
        timestamp = str(int(time.time()))
        workflow_desc = f"创建一个简单的测试工作流，包含开始、Agent执行和结束节点 - {timestamp}"

        print(f"3. 输入工作流描述: {workflow_desc}")
        page.fill("#aiWorkflowDesc", workflow_desc)

        # 点击生成按钮
        print("4. 点击'✨ 生成工作流'按钮...")
        page.click("button:has-text('生成工作流')")

        # 等待AI生成完成（最多60秒）
        print("5. 等待AI生成完成（最多60秒）...")
        try:
            page.wait_for_selector(".toast.success", timeout=60000)
            print("   ✓ 收到成功提示")
        except:
            print("   ⚠ 等待超时或未收到成功提示")

        page.wait_for_timeout(2000)

        # 验证结果
        print("6. 验证工作流是否创建成功...")

        # 检查是否有错误弹窗
        has_error = page.locator(".toast.error").count() > 0
        if has_error:
            error_text = page.locator(".toast.error").first.text_content()
            print(f"   ✗ 错误提示: {error_text}")

        # 检查画布是否有节点
        canvas_has_nodes = page.locator(".node").count() > 0
        if canvas_has_nodes:
            print(f"   ✓ 画布已加载 {page.locator('.node').count()} 个节点")
        else:
            print("   ✗ 画布没有节点")

        # 结果
        if canvas_has_nodes and not has_error:
            print("\n✅ 测试通过: AI生成工作流并加载成功")
            result = True
        else:
            print("\n❌ 测试失败: AI生成工作流或加载失败")
            result = False

        # 截图
        page.screenshot(path=f"/tmp/bug32_test_{timestamp}.png")
        print(f"截图已保存: /tmp/bug32_test_{timestamp}.png")

        # 关闭浏览器
        browser.close()

        return result

if __name__ == "__main__":
    result = test_ai_generate_workflow()
    exit(0 if result else 1)