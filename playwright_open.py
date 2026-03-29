from playwright.sync_api import sync_playwright
import time

USER_DATA_DIR = "/Users/weidian/.playwright-user-data"

p = sync_playwright().start()
browser = p.chromium.launch_persistent_context(
    user_data_dir=USER_DATA_DIR,
    headless=False
)

page = browser.new_page()
page.goto('https://air.1688.com/app/channel-fe/distribution-work/ai-assistant.html?#/multi-agent-common?inputContent=')

print('浏览器已打开，登录后请告知')
print('SESSION_FILE:/tmp/playwright_session.txt')

# 保存 browser 和 page 的引用以便后续使用
# 由于 Playwright 的限制，我们需要保持进程运行
print('浏览器保持打开，按 Ctrl+C 关闭...')
try:
    while True:
        time.sleep(1)
except KeyboardInterrupt:
    print('关闭浏览器')
    browser.close()
    p.stop()