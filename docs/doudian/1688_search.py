#!/usr/bin/env python3
"""
1688商品搜索脚本
功能：根据关键词搜索1688商品，返回商品列表（名称、价格、销量、链接）

使用方法：
    python 1688_search.py --keyword "逗猫棒" --pages 2

参数：
    --keyword: 搜索关键词（必需）
    --pages: 搜索页数（默认1）
"""

import asyncio
import argparse
import json
from playwright.async_api import async_playwright


async def search_1688(keyword: str, pages: int = 1):
    """搜索1688商品"""
    results = []

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=False)
        context = await browser.new_context()
        page = await context.new_page()

        # 搜索URL
        search_url = f"https://s.1688.com/selloffer/offer_search.htm?keywords={keyword}"
        print(f"搜索关键词: {keyword}")
        print(f"搜索URL: {search_url}")

        await page.goto(search_url, wait_until="networkidle")
        await page.wait_for_timeout(2000)

        for page_num in range(pages):
            print(f"\n=== 第 {page_num + 1} 页 ===")

            # 等待商品列表加载
            await page.wait_for_selector(".offer-list-item, .list-item", timeout=10000)
            await page.wait_for_timeout(1000)

            # 提取商品信息
            items = await page.query_selector_all(".offer-list-item, .list-item")
            print(f"找到 {len(items)} 个商品")

            for idx, item in enumerate(items):
                try:
                    # 商品名称
                    title_elem = await item.query_selector("a.title, .title a, .offer-title a")
                    title = await title_elem.inner_text() if title_elem else ""

                    # 商品链接（可能是加密链接）
                    link = ""
                    if title_elem:
                        link = await title_elem.get_attribute("href") or ""

                    # 价格
                    price_elem = await item.query_selector(".price, .price-value")
                    price = await price_elem.inner_text() if price_elem else ""

                    # 销量
                    sales_elem = await item.query_selector(".trade-number, .sales")
                    sales = await sales_elem.inner_text() if sales_elem else ""

                    if title and link:
                        product = {
                            "name": title.strip(),
                            "price": price.strip(),
                            "sales": sales.strip(),
                            "link": link.strip()
                        }
                        results.append(product)
                        print(f"{idx + 1}. {title[:30]}... | {price} | {sales}")

                except Exception as e:
                    print(f"提取商品 {idx + 1} 失败: {e}")

            # 下一页
            if page_num < pages - 1:
                next_btn = await page.query_selector("a.next, .next-page")
                if next_btn:
                    await next_btn.click()
                    await page.wait_for_timeout(2000)
                else:
                    print("没有更多页面")
                    break

        await browser.close()

    return results


def main():
    parser = argparse.ArgumentParser(description="1688商品搜索")
    parser.add_argument("--keyword", required=True, help="搜索关键词")
    parser.add_argument("--pages", type=int, default=1, help="搜索页数")

    args = parser.parse_args()

    results = asyncio.run(search_1688(args.keyword, args.pages))

    # 保存结果
    output_file = f"1688_search_{args.keyword}.json"
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)

    print(f"\n搜索完成，共 {len(results)} 个商品")
    print(f"结果已保存到: {output_file}")


if __name__ == "__main__":
    main()