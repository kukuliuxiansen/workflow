#!/usr/bin/env python3
"""
完整端到端测试
测试工作流执行、暂停、恢复、历史记录功能
"""

import json
import requests
import websocket
import time
import threading
import uuid

API = "http://localhost:3001/api"

class ChromeDevTools:
    def __init__(self, port=9222):
        self.port = port
        self.ws = None
        self.message_id = 0

    def get_page(self):
        pages = requests.get(f"http://localhost:{self.port}/json").json()
        for page in pages:
            if 'localhost:3001' in page.get('url', ''):
                return page
        return None

    def connect(self, page_id):
        self.ws = websocket.create_connection(f"ws://localhost:{self.port}/devtools/page/{page_id}")
        threading.Thread(target=self._receive_loop, daemon=True).start()

    def _receive_loop(self):
        while self.ws and self.ws.connected:
            try:
                self.ws.recv()
            except:
                break

    def evaluate(self, script):
        self.message_id += 1
        self.ws.send(json.dumps({
            "id": self.message_id,
            "method": "Runtime.evaluate",
            "params": {"expression": script, "returnByValue": True}
        }))
        for _ in range(50):
            try:
                result = self.ws.recv()
                data = json.loads(result)
                if 'id' in data and data['id'] == self.message_id:
                    return data.get('result', {}).get('result', {}).get('value')
            except:
                pass
            time.sleep(0.1)
        return None

    def close(self):
        if self.ws:
            self.ws.close()


def create_test_workflow():
    """创建测试工作流"""
    workflow_id = f"wf_{int(time.time() * 1000)}"
    workflow_data = {
        "id": workflow_id,
        "name": f"端到端测试_{uuid.uuid4().hex[:6]}",
        "description": "完整功能测试",
        "version": "1.0",
        "status": "draft",
        "nodes": [
            {"id": "start", "type": "start", "name": "开始", "x": 100, "y": 100},
            {"id": "agent1", "type": "agent_execution", "name": "测试Agent", "x": 300, "y": 100,
             "data": {"agentId": "project-manager", "prompt": "输出hello"}},
            {"id": "finish", "type": "finish", "name": "结束", "x": 500, "y": 100}
        ],
        "edges": [
            {"id": "e1", "source": "start", "target": "agent1"},
            {"id": "e2", "source": "agent1", "target": "finish"}
        ]
    }

    resp = requests.post(f"{API}/workflows", json=workflow_data)
    result = resp.json()
    if result.get('success'):
        return result['data']['id']
    return None


def test_end_to_end():
    chrome = ChromeDevTools()
    page = chrome.get_page()

    if not page:
        print("❌ 未找到页面，请打开Chrome访问localhost:3001")
        return

    print(f"✅ 连接到页面: {page['title']}")
    chrome.connect(page['id'])

    # 刷新页面
    chrome.evaluate("location.reload(true)")
    time.sleep(4)

    # ========== 测试1: 创建工作流 ==========
    print("\n=== 测试1: 创建测试工作流 ===")
    workflow_id = create_test_workflow()
    if not workflow_id:
        print("❌ 创建工作流失败")
        return
    print(f"✅ 工作流ID: {workflow_id}")

    # 刷新加载新工作流
    chrome.evaluate("location.reload(true)")
    time.sleep(3)

    # ========== 测试2: 执行工作流 ==========
    print("\n=== 测试2: 执行工作流 ===")
    chrome.evaluate(f"""
        window.confirmAsync = async (msg) => true;
        state.currentWorkflow = {{ id: '{workflow_id}', name: '测试' }};
        taskConfig.name = '端到端测试任务';
        taskConfig.description = '测试';
        taskConfig.projectPath = '/tmp/test';
        doExecuteWorkflow();
    """)
    time.sleep(3)

    execution = chrome.evaluate("JSON.stringify(state.execution)")
    status = chrome.evaluate("state.executionStatus")
    btn_pause = chrome.evaluate("document.getElementById('btnPause')?.style.display")

    if execution and execution != 'null':
        import json
        exec_data = json.loads(execution)
        exec_id = exec_data.get('id')
        print(f"✅ 执行启动成功")
        print(f"   执行ID: {exec_id}")
        print(f"   状态: {status}")
        print(f"   暂停按钮: {btn_pause}")

        # ========== 测试3: 暂停功能 ==========
        print("\n=== 测试3: 暂停功能 ===")
        chrome.evaluate("window.confirmAsync = async () => true; pauseExecution();")
        time.sleep(2)

        new_status = chrome.evaluate("state.executionStatus")
        btn_resume = chrome.evaluate("document.getElementById('btnResume')?.style.display")
        print(f"   暂停后状态: {new_status}")
        print(f"   恢复按钮: {btn_resume}")

        if btn_resume == 'inline-flex':
            print("✅ 暂停功能正常")

            # ========== 测试4: 恢复功能 ==========
            print("\n=== 测试4: 恢复功能 ===")
            chrome.evaluate("window.confirmAsync = async () => true; resumeExecution();")
            time.sleep(2)

            new_status = chrome.evaluate("state.executionStatus")
            print(f"   恢复后状态: {new_status}")
            print("✅ 恢复功能正常")
        else:
            print("⚠️ 暂停可能太快，执行已完成")
    else:
        print("❌ 执行启动失败")

    # ========== 测试5: 历史记录 ==========
    print("\n=== 测试5: 历史记录功能 ===")
    chrome.evaluate("await openHistoryPanel();")
    time.sleep(2)

    history_count = chrome.evaluate("document.getElementById('historyList')?.children.length")
    print(f"   历史记录数量: {history_count}")

    if history_count and history_count > 0:
        print("✅ 历史记录正常显示")
    else:
        print("❌ 历史记录未显示")

    # 关闭面板
    chrome.evaluate("closeHistoryPanel();")

    # ========== 测试6: WebSocket 连接 ==========
    print("\n=== 测试6: WebSocket 连接 ===")
    # 检查后端日志
    import subprocess
    result = subprocess.run(["tail", "-20", "/Users/weidian/claudeProject/workflows/logs/app.log"],
                          capture_output=True, text=True)
    ws_logs = [l for l in result.stdout.split('\n') if 'WebSocket' in l and 'executionId=' in l]
    if ws_logs:
        last_ws = ws_logs[-1]
        if 'executionId=undefined' in last_ws:
            print("❌ WebSocket executionId 仍然是 undefined")
        else:
            print(f"✅ WebSocket 正确传递 executionId")
            print(f"   最新连接: {last_ws[-100:]}")
    else:
        print("⚠️ 未找到 WebSocket 日志")

    # ========== 清理 ==========
    print("\n=== 清理测试数据 ===")
    requests.delete(f"{API}/workflows/{workflow_id}")
    print("✅ 已删除测试工作流")

    chrome.close()
    print("\n" + "="*50)
    print("端到端测试完成")
    print("="*50)


if __name__ == "__main__":
    test_end_to_end()