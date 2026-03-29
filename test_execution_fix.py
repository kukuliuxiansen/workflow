#!/usr/bin/env python3
"""
Chrome DevTools Protocol 客户端
测试工作流执行修复
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
        self.base_url = f"http://localhost:{port}"
        self.ws = None
        self.message_id = 0
        self.responses = {}

    def get_page(self):
        pages = requests.get(f"{self.base_url}/json").json()
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
                data = json.loads(self.ws.recv())
                if 'id' in data:
                    self.responses[data['id']] = data
            except:
                break

    def evaluate(self, script):
        self.message_id += 1
        self.ws.send(json.dumps({"id": self.message_id, "method": "Runtime.evaluate",
                                 "params": {"expression": script, "returnByValue": True}}))
        for _ in range(50):
            if self.message_id in self.responses:
                return self.responses.pop(self.message_id)
            time.sleep(0.1)
        return None

    def get_value(self, script):
        result = self.evaluate(script)
        return result.get('result', {}).get('result', {}).get('value')


def create_test_workflow():
    """创建测试工作流"""
    workflow_id = f"wf_{int(time.time() * 1000)}"
    workflow_data = {
        "id": workflow_id,
        "name": f"测试执行_{uuid.uuid4().hex[:6]}",
        "description": "测试执行功能",
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
    if result.get('success') and result.get('data'):
        return result['data']['id']
    print(f"创建失败: {result}")
    return None


def test_execution():
    chrome = ChromeDevTools()
    page = chrome.get_page()

    if not page:
        print("未找到页面，请打开Chrome访问localhost:3001")
        return

    print(f"连接到页面: {page['title']}")
    chrome.connect(page['id'])
    time.sleep(1)

    # 创建测试工作流
    print("\n=== 创建测试工作流 ===")
    workflow_id = create_test_workflow()
    if not workflow_id:
        print("创建失败，使用现有工作流")
        # 获取一个现有工作流
        resp = requests.get(f"{API}/workflows")
        workflows = resp.json().get('data', [])
        if workflows:
            workflow_id = workflows[0]['id']
            print(f"使用工作流: {workflow_id}")

    print(f"工作流ID: {workflow_id}")

    # 刷新页面
    chrome.evaluate("location.reload()")
    time.sleep(3)

    # 设置状态
    print("\n=== 设置前端状态 ===")

    # 重写 confirmAsync，设置任务配置
    chrome.evaluate(f"""
        window.confirmAsync = async (msg) => true;
        state.currentWorkflow = {{ id: '{workflow_id}', name: '测试' }};
        taskConfig.name = '测试任务';
        taskConfig.description = '自动测试';
        taskConfig.projectPath = '/tmp/test';
    """)

    # 验证设置
    name = chrome.get_value("taskConfig.name")
    path = chrome.get_value("taskConfig.projectPath")
    print(f"taskConfig.name: {name}, projectPath: {path}")

    # 执行工作流
    print("\n=== 执行工作流 ===")
    chrome.evaluate("doExecuteWorkflow()")

    # 等待API响应
    time.sleep(3)

    # 检查执行状态
    print("\n=== 检查执行状态 ===")
    execution = chrome.get_value("JSON.stringify(state.execution)")
    status = chrome.get_value("state.executionStatus")
    btn_execute = chrome.get_value("document.getElementById('btnExecute')?.style.display")
    btn_pause = chrome.get_value("document.getElementById('btnPause')?.style.display")
    btn_resume = chrome.get_value("document.getElementById('btnResume')?.style.display")

    print(f"execution: {execution}")
    print(f"status: {status}")
    print(f"btnExecute: {btn_execute}, btnPause: {btn_pause}, btnResume: {btn_resume}")

    if execution and execution != 'null':
        print("\n✅ 执行成功启动！")
        exec_data = json.loads(execution)
        exec_id = exec_data.get('id') or exec_data.get('executionId')
        print(f"执行ID: {exec_id}")

        # 测试暂停
        if btn_pause == 'inline-flex':
            print("\n=== 测试暂停 ===")
            chrome.evaluate("""
                window.confirmAsync = async () => true;
                pauseExecution();
            """)
            time.sleep(2)

            # 检查暂停后状态
            new_status = chrome.get_value("state.executionStatus")
            new_btn_pause = chrome.get_value("document.getElementById('btnPause')?.style.display")
            new_btn_resume = chrome.get_value("document.getElementById('btnResume')?.style.display")
            print(f"暂停后 - status: {new_status}, btnPause: {new_btn_pause}, btnResume: {new_btn_resume}")

            if new_btn_resume == 'inline-flex':
                print("✅ 暂停成功，恢复按钮已显示")

                # 测试恢复
                print("\n=== 测试恢复 ===")
                chrome.evaluate("""
                    window.confirmAsync = async () => true;
                    resumeExecution();
                """)
                time.sleep(2)

                new_status = chrome.get_value("state.executionStatus")
                print(f"恢复后 - status: {new_status}")
            else:
                print("⚠️ 暂停后恢复按钮未显示")
    else:
        print("\n❌ 执行启动失败")
        # 检查后端日志
        import subprocess
        result = subprocess.run(["tail", "-30", "/Users/weidian/claudeProject/workflows/logs/app.log"],
                              capture_output=True, text=True)
        print("后端日志:")
        print(result.stdout[-2000:])

    # 清理
    print("\n=== 清理 ===")
    requests.delete(f"{API}/workflows/{workflow_id}")
    print("已删除测试工作流")


if __name__ == "__main__":
    test_execution()