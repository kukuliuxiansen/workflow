from playwright.sync_api import sync_playwright
import time
import json
import os

USER_DATA_DIR = "/Users/weidian/.playwright-user-data"
COMMAND_FILE = "/tmp/playwright_command.json"
RESULT_FILE = "/tmp/playwright_result.json"

p = sync_playwright().start()

browser = p.chromium.launch_persistent_context(
    user_data_dir=USER_DATA_DIR,
    headless=False
)

page = browser.new_page()
page.goto('https://air.1688.com/app/channel-fe/distribution-work/ai-assistant.html?#/multi-agent-common?inputContent=')

print('BROWSER_READY')

def execute_command(cmd):
    """执行命令"""
    action = cmd.get('action')
    result = {'success': False, 'message': ''}

    try:
        if action == 'screenshot':
            path = cmd.get('path', '/tmp/screenshot.png')
            page.screenshot(path=path)
            result = {'success': True, 'message': f'截图已保存: {path}'}

        elif action == 'click':
            selector = cmd.get('selector')
            text = cmd.get('text')
            if text:
                element = page.locator(f'text={text}').first
            else:
                element = page.locator(selector)
            element.click()
            result = {'success': True, 'message': f'已点击: {text or selector}'}

        elif action == 'check':
            text = cmd.get('text')
            checkbox = page.locator(f'text={text}').first
            checkbox.click()
            result = {'success': True, 'message': f'已勾选: {text}'}

        elif action == 'wait':
            seconds = cmd.get('seconds', 2)
            time.sleep(seconds)
            result = {'success': True, 'message': f'等待 {seconds} 秒'}

        elif action == 'goto':
            url = cmd.get('url')
            page.goto(url)
            result = {'success': True, 'message': f'已打开: {url}'}

        elif action == 'content':
            content = page.content()
            result = {'success': True, 'message': '页面内容已获取', 'data': content[:5000]}

        elif action == 'list_elements':
            text = cmd.get('text', '')
            if text:
                elements = page.locator(f'text={text}')
            else:
                elements = page.locator('*')
            count = elements.count()
            result = {'success': True, 'message': f'找到 {count} 个元素'}

        elif action == 'find_checkbox':
            text = cmd.get('text', '')
            # 尝试多种方式找到复选框
            selectors = [
                f'label:has-text("{text}") input[type="checkbox"]',
                f'//label[contains(text(), "{text}")]//input[@type="checkbox"]',
                f'text={text} >> .. >> input[type="checkbox"]',
                f'[aria-label*="{text}"]',
            ]
            found = False
            for sel in selectors:
                try:
                    el = page.locator(sel).first
                    if el:
                        result = {'success': True, 'message': f'找到元素: {sel}'}
                        found = True
                        break
                except:
                    continue
            if not found:
                result = {'success': False, 'message': '未找到复选框'}

        elif action == 'check_checkbox':
            text = cmd.get('text', '')
            selectors = [
                f'label:has-text("{text}") input[type="checkbox"]',
                f'//label[contains(text(), "{text}")]//input[@type="checkbox"]',
            ]
            for sel in selectors:
                try:
                    checkbox = page.locator(sel).first
                    if checkbox:
                        checkbox.check()
                        result = {'success': True, 'message': f'已勾选: {text}'}
                        break
                except:
                    continue
            else:
                result = {'success': False, 'message': f'未找到复选框: {text}'}

        elif action == 'input':
            text = cmd.get('text', '')
            placeholder = cmd.get('placeholder', '')
            value = cmd.get('value', '')
            try:
                if placeholder:
                    input_el = page.locator(f'input[placeholder*="{placeholder}"]').first
                elif text:
                    input_el = page.locator(f'text={text}').first
                else:
                    input_el = page.locator('input').first
                input_el.fill(value)
                result = {'success': True, 'message': f'已输入: {value}'}
            except Exception as e:
                result = {'success': False, 'message': str(e)}

        elif action == 'press':
            key = cmd.get('key', 'Enter')
            page.keyboard.press(key)
            result = {'success': True, 'message': f'已按下: {key}'}

        elif action == 'type':
            value = cmd.get('value', '')
            page.keyboard.type(value)
            result = {'success': True, 'message': f'已输入: {value}'}

        elif action == 'click_selector':
            selector = cmd.get('selector', '')
            el = page.locator(selector).first
            el.click()
            result = {'success': True, 'message': f'已点击: {selector}'}

        elif action == 'fill':
            selector = cmd.get('selector', '')
            value = cmd.get('value', '')
            el = page.locator(selector).first
            el.fill(value)
            result = {'success': True, 'message': f'已填充: {value}'}

        elif action == 'focus_input':
            # 尝试聚焦任何可见的输入框
            selectors = ['textarea', 'input[type="text"]', 'input:not([type])', '[contenteditable="true"]']
            for sel in selectors:
                try:
                    el = page.locator(sel).first
                    if el:
                        el.focus()
                        result = {'success': True, 'message': f'已聚焦: {sel}'}
                        break
                except:
                    continue
            else:
                result = {'success': False, 'message': '未找到输入框'}

        elif action == 'click_dialog':
            # 点击按钮并捕获/处理 dialog 事件
            text = cmd.get('text', '')
            accept_value = cmd.get('accept_value', None)  # 如果提供，则接受并输入值

            dialogs = []
            console_msgs = []

            def handle_dialog(dialog):
                dialogs.append({
                    'type': dialog.type,
                    'message': dialog.message,
                    'defaultValue': dialog.default_value
                })
                print(f'DIALOG: type={dialog.type}, msg={dialog.message}, default={dialog.default_value}')

                if accept_value is not None:
                    dialog.accept(accept_value)
                else:
                    # 默认关闭 dialog
                    dialog.dismiss()

            def handle_console(msg):
                console_msgs.append({
                    'type': msg.type,
                    'text': msg.text
                })
                print(f'CONSOLE: {msg.type}: {msg.text}')

            page.on('dialog', handle_dialog)
            page.on('console', handle_console)

            try:
                if text:
                    element = page.locator(f'text={text}').first
                else:
                    element = page.locator('button').first
                element.click()

                # 等待 dialog 处理
                page.wait_for_timeout(1000)

                result = {
                    'success': True,
                    'message': '点击完成',
                    'dialogs': dialogs,
                    'dialogCount': len(dialogs),
                    'consoleMessages': console_msgs
                }
            except Exception as e:
                result = {'success': False, 'message': str(e), 'dialogs': dialogs, 'consoleMessages': console_msgs}
            finally:
                try:
                    page.remove_listener('dialog', handle_dialog)
                    page.remove_listener('console', handle_console)
                except:
                    pass

        else:
            result = {'success': False, 'message': f'未知命令: {action}'}

    except Exception as e:
        result = {'success': False, 'message': str(e)}

    return result

# 主循环：监听命令
try:
    while True:
        time.sleep(0.5)

        if os.path.exists(COMMAND_FILE):
            try:
                with open(COMMAND_FILE, 'r') as f:
                    cmd = json.load(f)

                # 删除命令文件
                os.remove(COMMAND_FILE)

                # 执行命令
                result = execute_command(cmd)

                # 写入结果
                with open(RESULT_FILE, 'w') as f:
                    json.dump(result, f, ensure_ascii=False)

                print(f'COMMAND_DONE: {cmd.get("action")}')

            except Exception as e:
                print(f'COMMAND_ERROR: {e}')

except KeyboardInterrupt:
    print('关闭浏览器')
    browser.close()
    p.stop()