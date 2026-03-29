#!/usr/bin/env python3
"""
渲染联动专项测试 - 简化版
直接测试关键渲染联动
"""

from playwright.sync_api import sync_playwright
import time

def test_render():
    results = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)
        page = browser.new_page()

        print("=" * 70)
        print("渲染联动专项测试")
        print("=" * 70)

        # 打开页面
        page.goto("http://localhost:3001/index.html")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(1500)

        # 关闭弹窗
        page.evaluate("document.querySelectorAll('.modal-overlay.show').forEach(m => m.classList.remove('show'))")
        page.wait_for_timeout(500)

        # ===== 测试1: 切换工作流 =====
        print("\n1. 切换工作流 - 历史面板应收起")
        workflows = page.locator(".workflow-item")
        if workflows.count() >= 1:
            workflows.first.click()
            page.wait_for_timeout(500)

            # 打开历史面板
            page.evaluate("document.getElementById('historyPanel').classList.add('show')")
            page.wait_for_timeout(200)

            # 切换工作流
            if workflows.count() >= 2:
                workflows.nth(1).click()
                page.wait_for_timeout(500)

                history_closed = not page.evaluate("document.getElementById('historyPanel').classList.contains('show')")
                print(f"   历史面板已关闭: {history_closed}")
                results.append(("切换工作流-历史面板关闭", history_closed))

                # 切回
                workflows.first.click()
                page.wait_for_timeout(300)

        # ===== 测试2: 切换工作流 - 右侧面板应收起 =====
        print("\n2. 切换工作流 - 右侧面板应收起")
        # 先展开右侧面板
        page.evaluate("document.getElementById('rightPanel').classList.remove('collapsed')")
        page.wait_for_timeout(200)

        if workflows.count() >= 2:
            workflows.nth(1).click()
            page.wait_for_timeout(500)

            right_collapsed = page.evaluate("document.getElementById('rightPanel').classList.contains('collapsed')")
            print(f"   右侧面板已收起: {right_collapsed}")
            results.append(("切换工作流-右侧面板收起", right_collapsed))

            # 切回
            workflows.first.click()
            page.wait_for_timeout(300)

        # ===== 测试3: 点击节点 - 右侧面板应展开 =====
        print("\n3. 点击节点 - 右侧面板应展开")
        # 先收起右侧面板
        page.evaluate("document.getElementById('rightPanel').classList.add('collapsed')")
        page.wait_for_timeout(200)

        nodes = page.locator(".node")
        if nodes.count() > 0:
            # 使用JS点击避免遮挡
            node_id = nodes.first.evaluate("el => el.id.replace('node-', '')")
            page.evaluate(f"selectNode('{node_id}')")
            page.wait_for_timeout(300)

            right_expanded = not page.evaluate("document.getElementById('rightPanel').classList.contains('collapsed')")
            print(f"   右侧面板已展开: {right_expanded}")
            results.append(("点击节点-右侧面板展开", right_expanded))

        # ===== 测试4: 属性面板内容 =====
        print("\n4. 属性面板内容")
        property_content = page.locator("#propertyPanelContent").text_content()
        has_content = "选择节点查看属性" not in property_content
        print(f"   属性面板有内容: {has_content}")
        results.append(("属性面板-有内容", has_content))

        # ===== 测试5: 取消选择 - 属性面板应清空 =====
        print("\n5. 取消选择 - 属性面板应清空")
        page.evaluate("state.selectedNode = null; renderPropertyPanel()")
        page.wait_for_timeout(200)

        property_content = page.locator("#propertyPanelContent").text_content()
        is_empty = "选择节点查看属性" in property_content
        print(f"   属性面板已清空: {is_empty}")
        results.append(("取消选择-属性面板清空", is_empty))

        # ===== 测试6: 添加节点弹窗 =====
        print("\n6. 添加节点弹窗")
        add_btn = page.locator("button:has-text('添加节点')")
        if add_btn.count() > 0:
            add_btn.click()
            page.wait_for_timeout(300)

            modal_visible = page.locator("#addNodeModal.show").count() > 0
            print(f"   弹窗显示: {modal_visible}")
            results.append(("添加节点-弹窗显示", modal_visible))

            # 关闭弹窗
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)
            # 确保弹窗关闭
            page.evaluate("document.querySelectorAll('.modal-overlay.show').forEach(m => m.classList.remove('show'))")
            page.wait_for_timeout(200)

        # ===== 测试7: 新建工作流弹窗 =====
        print("\n7. 新建工作流弹窗")
        new_btn = page.locator("button:has-text('新建工作流')")
        if new_btn.count() > 0:
            new_btn.click()
            page.wait_for_timeout(300)

            modal_visible = page.locator("#createModal.show").count() > 0
            print(f"   弹窗显示: {modal_visible}")
            results.append(("新建工作流-弹窗显示", modal_visible))

            # 关闭弹窗
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)
            page.evaluate("document.querySelectorAll('.modal-overlay.show').forEach(m => m.classList.remove('show'))")
            page.wait_for_timeout(200)

        # ===== 测试8: 模板弹窗 =====
        print("\n8. 模板弹窗")
        template_btn = page.locator("button:has-text('从模板创建')")
        if template_btn.count() > 0:
            template_btn.click()
            page.wait_for_timeout(300)

            modal_visible = page.locator("#templateModal.show").count() > 0
            print(f"   弹窗显示: {modal_visible}")

            templates = page.locator(".template-card").count()
            print(f"   模板数量: {templates}")
            results.append(("模板弹窗-显示", modal_visible))
            results.append(("模板弹窗-有模板", templates > 0))

            # 关闭弹窗
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)
            page.evaluate("document.querySelectorAll('.modal-overlay.show').forEach(m => m.classList.remove('show'))")
            page.wait_for_timeout(200)

        # ===== 测试9: 全局配置弹窗 =====
        print("\n9. 全局配置弹窗")
        config_btn = page.locator("button:has-text('全局配置')")
        if config_btn.count() > 0:
            config_btn.click()
            page.wait_for_timeout(300)

            modal_visible = page.locator("#globalConfigModal.show").count() > 0
            print(f"   弹窗显示: {modal_visible}")
            results.append(("全局配置-弹窗显示", modal_visible))

            # 关闭弹窗
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)
            page.evaluate("document.querySelectorAll('.modal-overlay.show').forEach(m => m.classList.remove('show'))")
            page.wait_for_timeout(200)

        # ===== 测试10: 任务配置弹窗 =====
        print("\n10. 任务配置弹窗")
        task_btn = page.locator("button:has-text('任务配置')")
        if task_btn.count() > 0:
            task_btn.click()
            page.wait_for_timeout(300)

            modal_visible = page.locator("#taskConfigModal.show").count() > 0
            print(f"   弹窗显示: {modal_visible}")
            results.append(("任务配置-弹窗显示", modal_visible))

            # 关闭弹窗
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)
            page.evaluate("document.querySelectorAll('.modal-overlay.show').forEach(m => m.classList.remove('show'))")
            page.wait_for_timeout(200)

        # ===== 测试11: 执行按钮初始状态 =====
        print("\n11. 执行按钮初始状态")
        btn_execute = page.locator("#btnExecute")
        btn_pause = page.locator("#btnPause")
        btn_resume = page.locator("#btnResume")

        execute_visible = btn_execute.count() > 0 and btn_execute.is_visible()
        pause_visible = btn_pause.count() > 0 and btn_pause.is_visible()
        resume_visible = btn_resume.count() > 0 and btn_resume.is_visible()

        print(f"   执行按钮可见: {execute_visible}")
        print(f"   暂停按钮可见: {pause_visible}")
        print(f"   继续按钮可见: {resume_visible}")

        results.append(("初始状态-执行按钮可见", execute_visible))
        results.append(("初始状态-暂停按钮隐藏", not pause_visible))
        results.append(("初始状态-继续按钮隐藏", not resume_visible))

        # ===== 测试12: 右键菜单 =====
        print("\n12. 右键菜单")
        nodes = page.locator(".node")
        if nodes.count() > 0:
            node_id = nodes.first.evaluate("el => el.id.replace('node-', '')")
            page.evaluate(f"showNodeContextMenu(new MouseEvent('contextmenu'), '{node_id}')")
            page.wait_for_timeout(300)

            menu_visible = page.locator(".context-menu.show").count() > 0
            print(f"   右键菜单显示: {menu_visible}")
            results.append(("右键菜单-显示", menu_visible))

            # 关闭
            page.evaluate("closeContextMenu()")
            page.wait_for_timeout(200)

        # ===== 测试13: 历史面板切换 =====
        print("\n13. 历史面板切换")
        # 使用JS打开历史面板
        page.evaluate("openHistoryPanel()")
        page.wait_for_timeout(300)

        history_open = page.locator("#historyPanel.show").count() > 0
        print(f"   历史面板打开: {history_open}")
        results.append(("历史按钮-面板展开", history_open))

        # 使用JS关闭历史面板
        page.evaluate("closeHistoryPanel()")
        page.wait_for_timeout(300)

        history_closed = page.locator("#historyPanel.show").count() == 0
        print(f"   历史面板关闭: {history_closed}")
        results.append(("历史按钮-面板收起", history_closed))

        # ===== 测试14: AI生成弹窗 =====
        print("\n14. AI生成弹窗")
        ai_btn = page.locator("button:has-text('AI生成')")
        if ai_btn.count() > 0:
            ai_btn.click()
            page.wait_for_timeout(300)

            modal_visible = page.locator("#aiGenerateModal.show").count() > 0
            print(f"   弹窗显示: {modal_visible}")
            results.append(("AI生成-弹窗显示", modal_visible))

            page.keyboard.press("Escape")
            page.wait_for_timeout(200)

        # ===== 结果汇总 =====
        print("\n" + "=" * 70)
        print("测试结果汇总")
        print("=" * 70)

        passed = sum(1 for _, r in results if r)
        failed = sum(1 for _, r in results if not r)

        for name, result in results:
            print(f"{'✅' if result else '❌'} {name}")

        print(f"\n总计: {passed} 通过, {failed} 失败")

        # 截图
        timestamp = int(time.time())
        page.screenshot(path=f"/tmp/render_test_{timestamp}.png")
        print(f"\n截图: /tmp/render_test_{timestamp}.png")

        browser.close()

        # 返回失败的测试
        failed_tests = [name for name, result in results if not result]
        return failed_tests

if __name__ == "__main__":
    failed = test_render()
    if failed:
        print("\n" + "=" * 70)
        print("需要修复的问题:")
        for i, name in enumerate(failed, 1):
            print(f"{i}. {name}")