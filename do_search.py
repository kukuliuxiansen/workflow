import json
import time
import os

COMMAND_FILE = "/tmp/playwright_command.json"
RESULT_FILE = "/tmp/playwright_result.json"

def send_command(cmd):
    with open(COMMAND_FILE, 'w') as f:
        json.dump(cmd, f)
    for _ in range(30):
        time.sleep(0.5)
        if os.path.exists(RESULT_FILE):
            with open(RESULT_FILE, 'r') as f:
                result = json.load(f)
            os.remove(RESULT_FILE)
            return result
    return {'success': False, 'message': '超时'}

# 用 textarea 或 placeholder 找输入框
print("查找并输入...")

# 先尝试点击输入区域，然后输入
result = send_command({'action': 'click', 'text': '输入选品需求'})
print(json.dumps(result, ensure_ascii=False))

time.sleep(1)

# 使用键盘输入
result = send_command({'action': 'type', 'value': '猫狗热门玩具'})
print(json.dumps(result, ensure_ascii=False))

time.sleep(1)

result = send_command({'action': 'screenshot'})
print(json.dumps(result, ensure_ascii=False))