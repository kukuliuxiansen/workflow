#!/usr/bin/env python3
"""
1688获取真实商品链接
功能：从加密链接（dj.1688.com/ci_bb）跳转获取真实的商品详情链接

使用方法：
    python 1688_get_real_link.py --url "https://dj.1688.com/ci_bb?..."
    python 1688_get_real_link.py --file urls.txt

参数：
    --url: 单个加密链接
    --file: 包含多个链接的文件（每行一个）
"""

import asyncio
import argparse
import json
from playwright.async_api import async_playwright


async def get_real_link(url: str, page) -> dict:
    """获取真实链接"""
    print(f"访问: {url[:60]}...")

    try:
        await page.goto(url, wait_until="networkidle", timeout=30000)
        await page.wait_for_timeout(1000)

        real_url = page.url

        # 提取offer ID
        offer_id = ""
        if "offer/" in real_url:
            import re
            match = re.search(r"offer/(\d+)", real_url)
            if match:
                offer_id = match.group(1)

        result = {
            "original_url": url,
            "real_url": real_url,
            "offer_id": offer_id
        }

        print(f"真实链接: {real_url}")
        print(f"Offer ID: {offer_id}")

        return result

    except Exception as e:
        print(f"获取失败: {e}")
        return {
            "original_url": url,
            "real_url": "",
            "offer_id": "",
            "error": str(e)
        }


async def process_urls(urls: list):
    """处理多个URL"""
    results = []

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=False)
        context = await browser.new_context()
        page = await context.new_page()

        for idx, url in enumerate(urls):
            print(f"\n=== {idx + 1}/{len(urls)} ===")
            result = await get_real_link(url, page)
            results.append(result)

        await browser.close()

    return results


def main():
    parser = argparse.ArgumentParser(description="获取1688真实商品链接")
    parser.add_argument("--url", help="单个加密链接")
    parser.add_argument("--file", help="包含链接的文件")

    args = parser.parse_args()

    urls = []
    if args.url:
        urls = [args.url]
    elif args.file:
        with open(args.file, "r", encoding="utf-8") as f:
            urls = [line.strip() for line in f if line.strip()]
    else:
        parser.print_help()
        return

    results = asyncio.run(process_urls(urls))

    # 保存结果
    output_file = "1688_real_links.json"
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)

    print(f"\n处理完成，共 {len(results)} 个链接")
    print(f"结果已保存到: {output_file}")

    # 输出成功的链接
    success = [r for r in results if r.get("real_url")]
    if success:
        print("\n成功的真实链接:")
        for r in success:
            print(r["real_url"])


if __name__ == "__main__":
    main()