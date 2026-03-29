#!/usr/bin/env python3
"""
自主前端测试与自愈代理
严格按照 PROTOCOL_AUTO_TEST.md 要求实现
- 无限循环测试
- 全维度感官监控（Console错误、弹窗、页面崩溃、网络错误）
- 防御性修复与回归测试
"""

import json
import requests
import websocket
import time
import threading
import random
import uuid
import sys
import os
from datetime import datetime

# ==================== 配置 ====================
API = "http://localhost:3001/api"
CHROME_PORT = 9222
LOG_FILE = "/tmp/chaos_test.log"
ERROR_LOG = "/tmp/chaos_errors.log"

# ==================== 全局状态 ====================
class TestState:
    def __init__(self):
        self.cycle = 0
        self.errors = []
        self.dialogs = []
        self.network_errors = []
        self.running = True
        self.test_workflow_id = None

state = TestState()

def log(msg, level="INFO"):
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
    line = f"[{timestamp}] [{level}] {msg}"
    print(line)
    with open(LOG_FILE, "a") as f:
        f.write(line + "\n")

def log_error(msg):
    log(msg, "ERROR")
    with open(ERROR_LOG, "a") as f:
        f.write(f"[{datetime.now().isoformat()}] {msg}\n")
    state.errors.append(msg)

# ==================== Chrome DevTools 客户端 ====================
class ChromeDevToolsClient:
    def __init__(self):
        self.ws = None
        self.msg_id = 0
        self.responses = {}
        self.events = []

    def connect(self):
        try:
            pages = requests.get(f"http://localhost:{CHROME_PORT}/json", timeout=5).json()
            page = None
            for p in pages:
                if 'localhost:3001' in p.get('url', ''):
                    page = p
                    break

            if not page:
                log("未找到目标页面", "WARN")
                return False

            ws_url = f"ws://localhost:{CHROME_PORT}/devtools/page/{page['id']}"
            self.ws = websocket.create_connection(ws_url, timeout=30)
            threading.Thread(target=self._receive_loop, daemon=True).start()

            # 启用必要的域
            self.send("Runtime.enable")
            self.send("Console.enable")
            self.send("Network.enable")
            self.send("Page.enable")

            log("已连接到 Chrome DevTools")
            return True
        except Exception as e:
            log(f"连接失败: {e}", "ERROR")
            return False

    def _receive_loop(self):
        while self.ws and self.ws.connected:
            try:
                data = json.loads(self.ws.recv())
                if 'id' in data:
                    self.responses[data['id']] = data
                elif 'method' in data:
                    self._handle_event(data)
            except Exception as e:
                if self.ws and self.ws.connected:
                    log(f"接收错误: {e}", "ERROR")
                break

    def _handle_event(self, event):
        method = event.get('method', '')
        params = event.get('params', {})

        # 【强制】监听 Console 错误
        if method == 'Runtime.consoleAPICalled':
            msg_type = params.get('type', '')
            args = params.get('args', [])
            if msg_type == 'error':
                msg_text = ' '.join([a.get('value', str(a)) for a in args])
                log_error(f"Console Error: {msg_text}")
                state.errors.append(f"Console: {msg_text}")

        # 【强制】监听 JS 异常
        elif method == 'Runtime.exceptionThrown':
            details = params.get('exceptionDetails', {})
            msg = details.get('text', 'Unknown exception')
            log_error(f"JS Exception: {msg}")
            state.errors.append(f"Exception: {msg}")

        # 【强制】监听弹窗 (通过 JavaScript dialog 事件)
        elif method == 'Page.javascriptDialogOpening':
            msg = params.get('message', '')
            log_error(f"Dialog Detected: {msg}")
            state.dialogs.append(msg)
            # 自动关闭弹窗
            self.send("Page.handleJavaScriptDialog", {"accept": True})

        # 【强制】监听网络错误
        elif method == 'Network.responseReceived':
            response = params.get('response', {})
            status = response.get('status', 200)
            url = response.get('url', '')
            # 忽略 favicon 等无关资源的 404
            if status >= 400 and 'favicon' not in url.lower() and 'manifest' not in url.lower():
                log_error(f"Network Error: {status} {url}")
                state.network_errors.append(f"{status}: {url}")

        # 监听页面崩溃
        elif method == 'Page.crashed':
            log_error("Page Crashed!")
            state.errors.append("Page Crashed")

        self.events.append(event)

    def send(self, method, params=None):
        if not self.ws:
            return None
        self.msg_id += 1
        msg = {"id": self.msg_id, "method": method, "params": params or {}}
        try:
            self.ws.send(json.dumps(msg))
            return self.msg_id
        except Exception as e:
            log(f"发送失败: {e}", "ERROR")
            return None

    def evaluate(self, script, timeout=10):
        msg_id = self.send("Runtime.evaluate", {"expression": script, "returnByValue": True})
        if not msg_id:
            return None

        start = time.time()
        while time.time() - start < timeout:
            if msg_id in self.responses:
                resp = self.responses.pop(msg_id)
                if 'result' in resp and 'result' in resp['result']:
                    return resp['result']['result'].get('value')
                elif 'error' in resp:
                    log(f"执行错误: {resp['error']}", "ERROR")
                    return None
            time.sleep(0.05)
        return None

    def navigate(self, url):
        self.send("Page.navigate", {"url": url})
        time.sleep(2)

    def reload(self, ignore_cache=True):
        self.send("Page.reload", {"ignoreCache": ignore_cache})
        time.sleep(3)

    def close(self):
        if self.ws:
            self.ws.close()

# ==================== 测试工作流管理 ====================
def create_test_workflow():
    """创建测试工作流"""
    wid = f"wf_chaos_{int(time.time()*1000)}"
    workflow = {
        "id": wid,
        "name": f"混沌测试_{uuid.uuid4().hex[:6]}",
        "description": "自动化混沌测试工作流",
        "version": "1.0",
        "status": "draft",
        "nodes": [
            {"id": "start", "type": "start", "name": "开始", "x": 100, "y": 100},
            {"id": "agent1", "type": "agent_execution", "name": "测试Agent", "x": 300, "y": 100,
             "data": {"agentId": "project-manager", "prompt": "输出测试结果"}},
            {"id": "finish", "type": "finish", "name": "结束", "x": 500, "y": 100}
        ],
        "edges": [
            {"id": "e1", "source": "start", "target": "agent1"},
            {"id": "e2", "source": "agent1", "target": "finish"}
        ]
    }

    try:
        resp = requests.post(f"{API}/workflows", json=workflow, timeout=10)
        result = resp.json()
        if result.get('success'):
            log(f"创建测试工作流: {result['data']['id']}")
            return result['data']['id']
    except Exception as e:
        log(f"创建工作流失败: {e}", "ERROR")
    return None

def delete_test_workflow(wid):
    """删除测试工作流"""
    if wid:
        try:
            requests.delete(f"{API}/workflows/{wid}", timeout=5)
            log(f"删除工作流: {wid}")
        except:
            pass

# ==================== 随机交互动作 ====================
def perform_random_actions(client, actions_count=5):
    """执行随机交互动作"""
    actions = [
        "click_workflow_item",
        "open_global_config",
        "close_modal",
        "add_log",
        "check_state",
        "execute_workflow",
        "open_history",
        "switch_tab"
    ]

    for _ in range(actions_count):
        if state.errors or state.dialogs:
            log("检测到错误，停止当前周期", "WARN")
            return False

        action = random.choice(actions)
        try:
            if action == "click_workflow_item":
                # 点击工作流列表项
                client.evaluate("""
                    const items = document.querySelectorAll('#workflowList .workflow-item');
                    if (items.length > 0) items[Math.floor(Math.random() * items.length)].click();
                """)
                time.sleep(0.5)

            elif action == "open_global_config":
                client.evaluate("if (typeof showGlobalConfigModal === 'function') showGlobalConfigModal()")
                time.sleep(0.5)
                client.evaluate("if (typeof closeGlobalConfigModal === 'function') closeGlobalConfigModal()")

            elif action == "close_modal":
                client.evaluate("""
                    document.querySelectorAll('.modal-overlay.show').forEach(m => m.classList.remove('show'));
                """)

            elif action == "add_log":
                client.evaluate(f'addLog("info", "混沌测试日志 {random.randint(1, 1000)}")')

            elif action == "check_state":
                client.evaluate("JSON.stringify({status: state.executionStatus, workflow: state.currentWorkflow?.id})")

            elif action == "execute_workflow":
                if state.test_workflow_id:
                    client.evaluate(f"""
                        (async function() {{
                            if (!state.currentWorkflow) {{
                                const res = await fetch('{API}/workflows/{state.test_workflow_id}');
                                const data = await res.json();
                                if (data.success) state.currentWorkflow = data.data;
                            }}
                            taskConfig.name = '混沌测试';
                            taskConfig.projectPath = '/tmp/test';
                            window.confirmAsync = async () => true;
                            if (typeof doExecuteWorkflow === 'function') doExecuteWorkflow();
                        }})()
                    """)
                    time.sleep(2)

            elif action == "open_history":
                client.evaluate("if (typeof openHistoryPanel === 'function') openHistoryPanel()")
                time.sleep(0.5)
                client.evaluate("if (typeof closeHistoryPanel === 'function') closeHistoryPanel()")

            elif action == "switch_tab":
                # 切换日志标签
                client.evaluate("""
                    const tabs = document.querySelectorAll('.log-main-tab');
                    if (tabs.length > 0) tabs[Math.floor(Math.random() * tabs.length)].click();
                """)

            time.sleep(random.uniform(0.2, 0.5))

        except Exception as e:
            log(f"动作执行错误: {e}", "ERROR")

    return True

def check_health_status(client):
    """检查健康状态"""
    result = client.evaluate("""
        (function() {
            return {
                hasState: typeof state !== 'undefined',
                hasLogs: typeof state !== 'undefined' && !!state.logs,
                executionStatus: typeof state !== 'undefined' ? state.executionStatus : 'unknown',
                errors: window.__testErrors || []
            };
        })()
    """)

    if result:
        log(f"健康检查: {result}")
        if result.get('errors'):
            for err in result['errors']:
                log_error(f"页面错误: {err}")

    return result

# ==================== 主测试循环 ====================
def run_chaos_test():
    """运行混沌测试"""
    log("=" * 60)
    log("自主前端测试启动")
    log("遵循 PROTOCOL_AUTO_TEST.md 协议")
    log("=" * 60)

    client = ChromeDevToolsClient()

    while state.running:
        state.cycle += 1
        state.errors = []
        state.dialogs = []
        state.network_errors = []

        log(f"\n{'='*60}")
        log(f"测试周期 #{state.cycle}")
        log(f"{'='*60}")

        try:
            # 连接/重连
            if not client.ws or not client.ws.connected:
                if not client.connect():
                    log("无法连接，等待重试...", "WARN")
                    time.sleep(5)
                    continue

            # 刷新页面
            log("刷新页面...")
            client.reload()

            # 创建测试工作流
            if not state.test_workflow_id:
                state.test_workflow_id = create_test_workflow()

            # 执行随机动作
            log("执行随机交互...")
            success = perform_random_actions(client, actions_count=10)

            if not success:
                log("周期失败，发现错误", "ERROR")
                return 1

            # 健康检查
            check_health_status(client)

            # 检查是否有错误
            if state.errors:
                log(f"发现 {len(state.errors)} 个错误", "ERROR")
                for err in state.errors:
                    log(f"  - {err}")
                return 1

            if state.dialogs:
                log(f"发现 {len(state.dialogs)} 个弹窗", "ERROR")
                for dlg in state.dialogs:
                    log(f"  - {dlg}")
                return 1

            if state.network_errors:
                log(f"发现 {len(state.network_errors)} 个网络错误", "WARN")

            log(f"周期 #{state.cycle} 完成，无错误")

            # 短暂休息
            time.sleep(2)

        except KeyboardInterrupt:
            log("用户中断")
            state.running = False
            break
        except Exception as e:
            log(f"周期异常: {e}", "ERROR")
            return 1

    # 清理
    delete_test_workflow(state.test_workflow_id)
    client.close()

    log("=" * 60)
    log(f"测试完成，共 {state.cycle} 个周期")
    log("=" * 60)
    return 0

if __name__ == "__main__":
    exit_code = run_chaos_test()
    sys.exit(exit_code)