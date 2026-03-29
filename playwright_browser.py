from playwright.sync_api import sync_playwright
import time

# 保持浏览器打开的脚本
p = sync_playwright().start()
browser = p.chromium.launch(headless=False)
page = browser.new_page()

# 打开目标页面
page.goto('https://air.1688.com/app/channel-fe/distribution-work/ai-assistant.html?#/multi-agent-common?inputContent=')
print('已打开页面:', page.url)
print('页面标题:', page.title())
print('浏览器保持打开，按 Ctrl+C 关闭...')

# 无限循环保持运行
try:
    while True:
        time.sleep(1)
except KeyboardInterrupt:
    print('用户中断，关闭浏览器')
    browser.close()
    p.stop()