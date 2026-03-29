#!/usr/bin/env python3
"""
1688商品详情提取
功能：打开1688商品详情页，提取完整商品信息

使用方法：
    python 1688_product_detail.py --url "https://detail.1688.com/offer/XXX.html"
    python 1688_product_detail.py --file urls.txt

参数：
    --url: 商品详情链接
    --file: 包含多个链接的文件（每行一个）
    --output: 输出文件名（默认 product_details.json）
"""

import asyncio
import argparse
import json
import re
from playwright.async_api import async_playwright


async def get_product_detail(url: str, page) -> dict:
    """获取商品详情"""
    print(f"获取商品: {url}")

    try:
        await page.goto(url, wait_until="networkidle", timeout=30000)
        await page.wait_for_timeout(2000)

        # 提取商品信息
        result = {"url": url}

        # 商品标题
        title_elem = await page.query_selector("h1.title, .d-title, .mod-detail-title h1")
        result["title"] = await title_elem.inner_text() if title_elem else ""

        # 价格
        price_elem = await page.query_selector(".price-value, .d-price .value")
        result["price"] = await price_elem.inner_text() if price_elem else ""

        # 起订量
        min_order_elem = await page.query_selector(".min-order, .d-min-order")
        result["min_order"] = await min_order_elem.inner_text() if min_order_elem else ""

        # 成交量
        sales_elem = await page.query_selector(".trade-number, .d-trade-number")
        result["sales"] = await sales_elem.inner_text() if sales_elem else ""

        # 店铺名称
        shop_elem = await page.query_selector(".shop-name, .d-shop-name a")
        result["shop_name"] = await shop_elem.inner_text() if shop_elem else ""

        # 提取Offer ID
        match = re.search(r"offer/(\d+)", url)
        result["offer_id"] = match.group(1) if match else ""

        # 商品图片
        images = []
        img_elements = await page.query_selector_all(".detail-gallery img, .d-content img")
        for img in img_elements[:5]:  # 只取前5张
            src = await img.get_attribute("src")
            if src:
                images.append(src)
        result["images"] = images

        print(f"标题: {result['title'][:40]}...")
        print(f"价格: {result['price']}")
        print(f"销量: {result['sales']}")

        return result

    except Exception as e:
        print(f"获取失败: {e}")
        return {"url": url, "error": str(e)}


async def process_products(urls: list):
    """处理多个商品"""
    results = []

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=False)
        context = await browser.new_context()
        page = await context.new_page()

        for idx, url in enumerate(urls):
            print(f"\n=== {idx + 1}/{len(urls)} ===")
            result = await get_product_detail(url, page)
            results.append(result)
            await page.wait_for_timeout(500)  # 短暂等待

        await browser.close()

    return results


def main():
    parser = argparse.ArgumentParser(description="提取1688商品详情")
    parser.add_argument("--url", help="商品详情链接")
    parser.add_argument("--file", help="包含链接的文件")
    parser.add_argument("--output", default="product_details.json", help="输出文件名")

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

    results = asyncio.run(process_products(urls))

    # 保存结果
    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)

    print(f"\n处理完成，共 {len(results)} 个商品")
    print(f"结果已保存到: {args.output}")


if __name__ == "__main__":
    main()