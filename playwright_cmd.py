#!/usr/bin/env python3
import json
import sys
import time
import os

COMMAND_FILE = "/tmp/playwright_command.json"
RESULT_FILE = "/tmp/playwright_result.json"

def send_command(cmd):
    """发送命令到浏览器进程"""
    with open(COMMAND_FILE, 'w') as f:
        json.dump(cmd, f)

    # 等待结果
    for _ in range(30):  # 最多等待15秒
        time.sleep(0.5)
        if os.path.exists(RESULT_FILE):
            with open(RESULT_FILE, 'r') as f:
                result = json.load(f)
            os.remove(RESULT_FILE)
            return result

    return {'success': False, 'message': '超时'}

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("用法: python playwright_cmd.py <action> [args]")
        print("  screenshot - 截图")
        print("  click <text> - 点击包含文本的元素")
        print("  check <text> - 勾选")
        print("  wait <seconds> - 等待")
        print("  content - 获取页面内容")
        sys.exit(1)

    action = sys.argv[1]

    if action == 'screenshot':
        result = send_command({'action': 'screenshot', 'path': '/tmp/screenshot.png'})
    elif action == 'click':
        result = send_command({'action': 'click', 'text': sys.argv[2]})
    elif action == 'check':
        result = send_command({'action': 'check', 'text': sys.argv[2]})
    elif action == 'wait':
        result = send_command({'action': 'wait', 'seconds': int(sys.argv[2]) if len(sys.argv) > 2 else 2})
    elif action == 'content':
        result = send_command({'action': 'content'})
    else:
        result = {'success': False, 'message': f'未知命令: {action}'}

    print(json.dumps(result, ensure_ascii=False, indent=2))