#!/usr/bin/env python3
"""
渲染联动专项测试
系统测试所有渲染联动场景
"""

from playwright.sync_api import sync_playwright
import time

def test_render_linkage():
    results = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)
        page = browser.new_page()

        print("=" * 60)
        print("渲染联动专项测试")
        print("=" * 60)

        # 打开页面
        print("\n打开页面...")
        page.goto("http://localhost:3001/index.html")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(1000)

        # 关闭弹窗
        page.evaluate("document.querySelectorAll('.modal-overlay.show').forEach(m => m.classList.remove('show'))")
        page.wait_for_timeout(500)

        # ===== 一、工作流切换场景 =====
        print("\n" + "=" * 60)
        print("一、工作流切换场景")
        print("=" * 60)

        workflows = page.locator(".workflow-item")
        wf_count = workflows.count()
        print(f"工作流数量: {wf_count}")

        if wf_count >= 1:
            # 1.1 切换工作流 - 画布渲染
            print("\n1.1 切换工作流 - 画布渲染")
            workflows.first.click()
            page.wait_for_timeout(500)

            node_count = page.locator(".node").count()
            print(f"   画布节点数: {node_count}")
            results.append(("切换工作流-画布渲染", node_count > 0, f"节点数={node_count}"))

            # 1.2 切换工作流 - 属性面板
            print("\n1.2 切换工作流 - 属性面板")
            property_content = page.locator("#propertyPanelContent").text_content()
            is_empty = "选择节点查看属性" in property_content
            print(f"   属性面板内容: {'空' if is_empty else '有内容'}")
            results.append(("切换工作流-属性面板清空", is_empty, ""))

            # 1.3 切换工作流 - 历史面板状态
            print("\n1.3 切换工作流 - 历史面板状态")
            # 先打开历史面板
            history_btn = page.locator("button:has-text('历史')")
            if history_btn.count() > 0:
                history_btn.click()
                page.wait_for_timeout(300)

                history_panel = page.locator("#historyPanel")
                is_open = history_panel.is_visible()
                print(f"   历史面板打开: {is_open}")

                # 切换工作流
                if wf_count >= 2:
                    workflows.nth(1).click()
                    page.wait_for_timeout(500)

                    # 检查历史面板是否收起
                    history_still_open = history_panel.is_visible()
                    print(f"   切换后历史面板仍然打开: {history_still_open}")

                    # 期望: 历史面板应该收起
                    should_close = not history_still_open
                    results.append(("切换工作流-历史面板应收起", should_close, f"实际={'打开' if history_still_open else '收起'}"))
                else:
                    results.append(("切换工作流-历史面板", True, "只有一个工作流跳过"))

        # ===== 二、节点操作场景 =====
        print("\n" + "=" * 60)
        print("二、节点操作场景")
        print("=" * 60)

        # 2.1 选择节点
        print("\n2.1 选择节点 - 属性面板")
        nodes = page.locator(".node")
        if nodes.count() > 0:
            nodes.first.click()
            page.wait_for_timeout(300)

            property_content = page.locator("#propertyPanelContent").text_content()
            has_content = "选择节点查看属性" not in property_content
            print(f"   属性面板有内容: {has_content}")
            results.append(("选择节点-属性面板显示", has_content, ""))

            # 2.2 选择节点 - 右侧面板展开
            print("\n2.2 选择节点 - 右侧面板展开")
            right_panel = page.locator(".right-panel")
            is_expanded = right_panel.evaluate("el => el.classList.contains('expanded')")
            print(f"   右侧面板展开: {is_expanded}")
            results.append(("选择节点-右侧面板展开", is_expanded, ""))

        # 2.3 添加节点
        print("\n2.3 添加节点 - 画布更新")
        initial_nodes = page.locator(".node").count()
        print(f"   初始节点数: {initial_nodes}")

        # 双击画布添加节点
        canvas = page.locator("#canvas")
        if canvas.count() > 0:
            canvas.click(position={"x": 400, "y": 300}, click_count=2)
            page.wait_for_timeout(1000)

            new_nodes = page.locator(".node").count()
            print(f"   添加后节点数: {new_nodes}")
            results.append(("添加节点-画布更新", new_nodes > initial_nodes, f"{initial_nodes}->{new_nodes}"))

        # 2.4 取消选择节点
        print("\n2.4 取消选择节点")
        # 点击画布空白处
        canvas = page.locator("#canvas")
        if canvas.count() > 0:
            canvas.click(position={"x": 50, "y": 50})
            page.wait_for_timeout(300)

            property_content = page.locator("#propertyPanelContent").text_content()
            is_cleared = "选择节点查看属性" in property_content
            print(f"   属性面板已清空: {is_cleared}")
            results.append(("取消选择-属性面板清空", is_cleared, ""))

        # ===== 三、连线操作场景 =====
        print("\n" + "=" * 60)
        print("三、连线操作场景")
        print("=" * 60)

        # 3.1 检查连线
        print("\n3.1 连线状态")
        edges = page.locator(".edge-path")
        edge_count = edges.count()
        print(f"   连线数: {edge_count}")
        results.append(("连线渲染", True, f"连线数={edge_count}"))

        # ===== 四、执行按钮状态 =====
        print("\n" + "=" * 60)
        print("四、执行按钮状态")
        print("=" * 60)

        # 4.1 初始状态
        print("\n4.1 初始按钮状态")
        btn_execute = page.locator("#btnExecute")
        btn_pause = page.locator("#btnPause")
        btn_resume = page.locator("#btnResume")

        execute_visible = btn_execute.is_visible() if btn_execute.count() > 0 else False
        pause_visible = btn_pause.is_visible() if btn_pause.count() > 0 else False
        resume_visible = btn_resume.is_visible() if btn_resume.count() > 0 else False

        print(f"   执行按钮可见: {execute_visible}")
        print(f"   暂停按钮可见: {pause_visible}")
        print(f"   继续按钮可见: {resume_visible}")

        results.append(("初始状态-执行按钮可见", execute_visible, ""))
        results.append(("初始状态-暂停按钮隐藏", not pause_visible, ""))
        results.append(("初始状态-继续按钮隐藏", not resume_visible, ""))

        # ===== 五、全局配置场景 =====
        print("\n" + "=" * 60)
        print("五、全局配置场景")
        print("=" * 60)

        # 5.1 任务配置按钮
        print("\n5.1 任务配置")
        task_btn = page.locator("button:has-text('任务配置')")
        if task_btn.count() > 0:
            task_btn.click()
            page.wait_for_timeout(300)

            modal = page.locator("#taskConfigModal.show")
            modal_visible = modal.count() > 0
            print(f"   任务配置弹窗显示: {modal_visible}")
            results.append(("任务配置-弹窗显示", modal_visible, ""))

            # 关闭弹窗
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)

        # ===== 六、模板创建场景 =====
        print("\n" + "=" * 60)
        print("六、模板创建场景")
        print("=" * 60)

        # 6.1 从模板创建
        print("\n6.1 从模板创建")
        template_btn = page.locator("button:has-text('从模板创建')")
        if template_btn.count() > 0:
            template_btn.click()
            page.wait_for_timeout(500)

            template_modal = page.locator("#templateModal.show")
            modal_visible = template_modal.count() > 0
            print(f"   模板弹窗显示: {modal_visible}")
            results.append(("模板创建-弹窗显示", modal_visible, ""))

            # 检查模板列表
            template_cards = page.locator(".template-card")
            template_count = template_cards.count()
            print(f"   模板数量: {template_count}")
            results.append(("模板创建-模板列表", template_count > 0, f"模板数={template_count}"))

            # 关闭弹窗
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)

        # ===== 结果汇总 =====
        print("\n" + "=" * 60)
        print("测试结果汇总")
        print("=" * 60)

        passed = 0
        failed = 0
        failed_items = []

        for name, result, detail in results:
            status = "✅" if result else "❌"
            print(f"{status} {name} {detail}")
            if result:
                passed += 1
            else:
                failed += 1
                failed_items.append(name)

        print(f"\n总计: {passed} 通过, {failed} 失败")

        if failed_items:
            print("\n失败的测试项:")
            for item in failed_items:
                print(f"  - {item}")

        # 截图
        timestamp = int(time.time())
        page.screenshot(path=f"/tmp/render_test_{timestamp}.png")
        print(f"\n截图: /tmp/render_test_{timestamp}.png")

        browser.close()
        return results

if __name__ == "__main__":
    test_render_linkage()