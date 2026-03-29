from playwright.sync_api import sync_playwright
import time
import sys

# 使用持久化用户数据目录，保存登录状态
USER_DATA_DIR = "/Users/weidian/.playwright-user-data"

p = sync_playwright().start()
browser = p.chromium.launch_persistent_context(
    user_data_dir=USER_DATA_DIR,
    headless=False
)

page = browser.new_page()
page.goto('https://air.1688.com/app/channel-fe/distribution-work/ai-assistant.html?#/multi-agent-common?inputContent=')

print('浏览器已打开，等待用户操作...')
print('如果需要登录，请登录后按回车继续')

# 等待用户输入
input('登录完成后按回车继续...')

# 查找 AI选品 复选框并勾选
try:
    # 等待页面加载
    time.sleep(2)

    # 尝试查找包含"AI选品"的元素
    checkbox = page.locator('text=AI选品').first
    if checkbox:
        checkbox.click()
        print('已点击 AI选品')
    else:
        print('未找到 AI选品 元素')

except Exception as e:
    print(f'操作失败: {e}')
    page.screenshot(path='/tmp/screenshot.png')
    print('已保存截图到 /tmp/screenshot.png')

# 保持浏览器打开
print('浏览器保持打开，按 Ctrl+C 关闭...')
try:
    while True:
        time.sleep(1)
except KeyboardInterrupt:
    print('关闭浏览器')
    browser.close()
    p.stop()