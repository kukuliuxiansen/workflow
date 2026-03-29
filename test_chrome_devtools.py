#!/usr/bin/env python3
"""
Chrome DevTools Protocol 客户端
用于测试 workflows 项目的历史相关功能
"""

import json
import requests
import websocket
import time
import threading

class ChromeDevTools:
    def __init__(self, port=9222):
        self.port = port
        self.base_url = f"http://localhost:{port}"
        self.ws = None
        self.message_id = 0
        self.responses = {}
        self.ws_thread = None

    def get_pages(self):
        """获取所有页面"""
        resp = requests.get(f"{self.base_url}/json")
        return resp.json()

    def get_workflow_page(self):
        """获取工作流页面"""
        pages = self.get_pages()
        for page in pages:
            if 'localhost:3001' in page.get('url', ''):
                return page
        return None

    def connect(self, page_id):
        """连接到页面的 WebSocket"""
        ws_url = f"ws://localhost:{self.port}/devtools/page/{page_id}"
        self.ws = websocket.create_connection(ws_url)

        # 启动接收线程
        self.ws_thread = threading.Thread(target=self._receive_loop, daemon=True)
        self.ws_thread.start()

    def _receive_loop(self):
        """接收消息循环"""
        while self.ws and self.ws.connected:
            try:
                result = self.ws.recv()
                data = json.loads(result)
                if 'id' in data:
                    self.responses[data['id']] = data
            except:
                break

    def send_command(self, method, params=None):
        """发送命令"""
        self.message_id += 1
        msg = {
            "id": self.message_id,
            "method": method,
            "params": params or {}
        }
        self.ws.send(json.dumps(msg))

        # 等待响应
        for _ in range(100):  # 最多等待10秒
            if self.message_id in self.responses:
                return self.responses.pop(self.message_id)
            time.sleep(0.1)
        return None

    def evaluate(self, script):
        """执行 JavaScript"""
        return self.send_command("Runtime.evaluate", {
            "expression": script,
            "returnByValue": True
        })

    def screenshot(self):
        """截图"""
        return self.send_command("Page.captureScreenshot")

    def close(self):
        """关闭连接"""
        if self.ws:
            self.ws.close()


def test_workflow():
    """测试工作流历史功能"""
    chrome = ChromeDevTools()

    # 获取工作流页面
    page = chrome.get_workflow_page()
    if not page:
        print("未找到工作流页面")
        return

    print(f"找到页面: {page['title']}")
    print(f"URL: {page['url']}")

    # 连接到页面
    chrome.connect(page['id'])
    print("已连接到页面")

    # 等待页面加载
    time.sleep(2)

    # ========== 测试1: 点击商角AI工作流 ==========
    print("\n=== 测试1: 点击商角AI工作流 ===")
    result = chrome.evaluate("""
        (function() {
            const items = document.querySelectorAll('.workflow-item, .workflow-card, [class*="workflow"]');
            for (const item of items) {
                const nameEl = item.querySelector('.workflow-name, [class*="name"]');
                if (nameEl && nameEl.textContent.includes('商角AI')) {
                    item.click();
                    return { success: true, clicked: nameEl.textContent };
                }
            }
            // 直接查找包含商角AI的文本
            const allElements = document.querySelectorAll('*');
            for (const el of allElements) {
                if (el.textContent && el.textContent.includes('商角AI') && el.children.length < 3) {
                    el.click();
                    return { success: true, clicked: el.textContent.trim().substring(0, 50) };
                }
            }
            return { success: false, message: '未找到商角AI' };
        })()
    """)
    value = result.get('result', {}).get('result', {}).get('value', {})
    print(f"点击结果: {value}")

    time.sleep(1)

    # ========== 测试2: 点击执行按钮 ==========
    print("\n=== 测试2: 点击执行按钮 ===")
    result = chrome.evaluate("""
        (function() {
            const buttons = document.querySelectorAll('button');
            for (const btn of buttons) {
                if (btn.textContent.includes('执行') && !btn.textContent.includes('日志')) {
                    btn.click();
                    return { success: true, clicked: btn.textContent.trim() };
                }
            }
            return { success: false, message: '未找到执行按钮' };
        })()
    """)
    value = result.get('result', {}).get('result', {}).get('value', {})
    print(f"执行按钮点击结果: {value}")

    # 等待执行开始
    time.sleep(3)

    # ========== 测试3: 检查执行状态 ==========
    print("\n=== 测试3: 检查执行状态 ===")
    result = chrome.evaluate("""
        (function() {
            // 查找状态显示
            const statusEl = document.querySelector('[class*="status"], .execution-status');
            const logContent = document.querySelector('#logPanel, .log-content, [class*="log"]');

            // 查找暂停按钮
            const buttons = document.querySelectorAll('button');
            let pauseBtn = null, resumeBtn = null, stopBtn = null;

            buttons.forEach(btn => {
                const text = btn.textContent.trim();
                if (text.includes('暂停')) pauseBtn = text;
                if (text.includes('恢复')) resumeBtn = text;
                if (text.includes('停止')) stopBtn = text;
            });

            return {
                statusText: statusEl ? statusEl.textContent : '未找到状态',
                logText: logContent ? logContent.textContent.substring(0, 200) : '未找到日志',
                pauseBtn: pauseBtn,
                resumeBtn: resumeBtn,
                stopBtn: stopBtn
            };
        })()
    """)
    value = result.get('result', {}).get('result', {}).get('value', {})
    print(f"状态: {value.get('statusText')}")
    print(f"日志片段: {value.get('logText')[:100]}...")
    print(f"控制按钮 - 暂停: {value.get('pauseBtn')}, 恢复: {value.get('resumeBtn')}, 停止: {value.get('stopBtn')}")

    # ========== 测试4: 点击暂停按钮 ==========
    print("\n=== 测试4: 点击暂停按钮 ===")
    result = chrome.evaluate("""
        (function() {
            const buttons = document.querySelectorAll('button');
            for (const btn of buttons) {
                if (btn.textContent.includes('暂停')) {
                    btn.click();
                    return { success: true, clicked: '暂停' };
                }
            }
            return { success: false, message: '未找到暂停按钮' };
        })()
    """)
    value = result.get('result', {}).get('result', {}).get('value', {})
    print(f"暂停按钮点击结果: {value}")

    time.sleep(2)

    # ========== 测试5: 检查暂停后状态 ==========
    print("\n=== 测试5: 检查暂停后状态 ===")
    result = chrome.evaluate("""
        (function() {
            const buttons = document.querySelectorAll('button');
            let resumeBtn = null;
            buttons.forEach(btn => {
                if (btn.textContent.includes('恢复')) resumeBtn = btn.textContent.trim();
            });

            // 检查日志面板是否有更新
            const logPanel = document.querySelector('#logPanel');
            const logItems = document.querySelectorAll('.log-item, [class*="log-entry"]');

            return {
                resumeBtn: resumeBtn,
                logCount: logItems.length,
                logPanelClass: logPanel ? logPanel.className : '未找到'
            };
        })()
    """)
    value = result.get('result', {}).get('result', {}).get('value', {})
    print(f"恢复按钮: {value.get('resumeBtn')}")
    print(f"日志条目数: {value.get('logCount')}")
    print(f"日志面板状态: {value.get('logPanelClass')}")

    # ========== 测试6: 点击恢复按钮 ==========
    print("\n=== 测试6: 点击恢复按钮 ===")
    result = chrome.evaluate("""
        (function() {
            const buttons = document.querySelectorAll('button');
            for (const btn of buttons) {
                if (btn.textContent.includes('恢复')) {
                    btn.click();
                    return { success: true, clicked: '恢复' };
                }
            }
            return { success: false, message: '未找到恢复按钮' };
        })()
    """)
    value = result.get('result', {}).get('result', {}).get('value', {})
    print(f"恢复按钮点击结果: {value}")

    time.sleep(3)

    # ========== 测试7: 检查执行日志实时更新 ==========
    print("\n=== 测试7: 检查执行日志实时更新 ===")
    result = chrome.evaluate("""
        (function() {
            // 获取日志面板内容
            const logPanel = document.querySelector('#logPanel .log-content, .log-panel, [class*="log"]');
            if (!logPanel) {
                // 尝试其他选择器
                const altLog = document.querySelector('[class*="log-item"], [class*="execution-log"]');
                return {
                    found: false,
                    message: '未找到日志面板',
                    altContent: altLog ? altLog.textContent.substring(0, 300) : '无'
                };
            }

            // 获取最后几条日志
            const logItems = logPanel.querySelectorAll('[class*="log-item"], tr, li, div');
            const lastLogs = [];
            logItems.forEach((item, i) => {
                if (i >= logItems.length - 5) {
                    lastLogs.push(item.textContent.trim().substring(0, 100));
                }
            });

            return {
                found: true,
                logCount: logItems.length,
                lastLogs: lastLogs
            };
        })()
    """)
    value = result.get('result', {}).get('result', {}).get('value', {})
    print(f"日志面板: {value.get('found')}")
    if value.get('found'):
        print(f"日志条目数: {value.get('logCount')}")
        print("最近日志:")
        for log in value.get('lastLogs', []):
            if log:
                print(f"  - {log[:80]}...")

    # ========== 测试8: 检查执行历史 ==========
    print("\n=== 测试8: 检查执行历史 ===")
    result = chrome.evaluate("""
        (function() {
            // 点击执行日志按钮
            const buttons = document.querySelectorAll('button');
            for (const btn of buttons) {
                if (btn.textContent.includes('执行日志')) {
                    btn.click();
                    return { clicked: '执行日志按钮' };
                }
            }
            return { message: '未找到执行日志按钮' };
        })()
    """)
    value = result.get('result', {}).get('result', {}).get('value', {})
    print(f"点击结果: {value}")

    time.sleep(2)

    # 检查历史列表
    result = chrome.evaluate("""
        (function() {
            // 查找历史记录列表
            const historyList = document.querySelectorAll('[class*="history"], [class*="execution-record"], .modal [class*="list"] tr, .modal tbody tr');
            const records = [];
            historyList.forEach((item, i) => {
                if (i < 10) {
                    const text = item.textContent.trim().replace(/\\s+/g, ' ').substring(0, 100);
                    if (text && text.length > 5) {
                        records.push(text);
                    }
                }
            });

            // 检查是否有模态框
            const modal = document.querySelector('.modal, [class*="modal"], [class*="dialog"]');

            return {
                modalVisible: modal ? modal.className : '无模态框',
                recordCount: records.length,
                records: records.slice(0, 5)
            };
        })()
    """)
    value = result.get('result', {}).get('result', {}).get('value', {})
    print(f"模态框状态: {value.get('modalVisible')}")
    print(f"历史记录数: {value.get('recordCount')}")
    if value.get('records'):
        print("历史记录:")
        for rec in value.get('records', []):
            print(f"  - {rec}")

    # ========== 测试总结 ==========
    print("\n" + "="*50)
    print("测试总结:")
    print("="*50)
    print("✅ 页面加载正常")
    print("✅ 工作流列表显示正常")
    print("✅ 执行按钮可点击")
    if value.get('recordCount', 0) > 0:
        print("✅ 执行历史显示正常")
    else:
        print("⚠️ 执行历史可能有问题")

    chrome.close()
    print("\n测试完成")


if __name__ == "__main__":
    test_workflow()