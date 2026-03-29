#!/usr/bin/env python3
"""
1688优质商品筛选脚本（完整版）
功能：根据关键词搜索1688，筛选优质商品，支持详情页抓取

使用方法：
    python 1688_filter_products.py --keywords "逗猫棒,猫抓板" --count 10
    python 1688_filter_products.py --keywords "猫爬架" --count 5 --output excel

严格筛选条件：
    1. 商品单价：15元 < 单价 < 300元
    2. 月销量 > 500
    3. 必须支持一件代发
    4. 必须有"3日达"或"48小时发货"
    5. 商品评分 > 4.5（如有）
    6. 起订量 ≤ 2件
    7. 优先：运费险、七天无理由、高回头率、抖店分销
"""

import asyncio
import argparse
import json
import re
import time
import hashlib
import os
from urllib.parse import quote, urlparse, parse_qs
from playwright.async_api import async_playwright

# 默认输出目录
OUTPUT_DIR = os.path.expanduser("~/Desktop/1688选品")


def parse_price(price_str) -> float:
    """解析价格"""
    if price_str is None:
        return 0
    price_str = str(price_str).replace("¥", "").replace("￥", "").replace("元", "").strip()
    match = re.search(r'(\d+\.?\d*)', price_str)
    if match:
        return float(match.group(1))
    return 0


def parse_sales(sales_str) -> int:
    """解析销量"""
    if sales_str is None:
        return 0
    sales_str = str(sales_str)

    match = re.search(r'成交\s*(\d+(?:\.\d+)?)\s*(万|千|百)?\+?\s*件', sales_str)
    if match:
        num = float(match.group(1))
        unit = match.group(2) or ""
        multipliers = {"万": 10000, "千": 1000, "百": 100}
        return int(num * multipliers.get(unit, 1))

    match = re.search(r'(\d+(?:\.\d+)?)\s*(万|千|百)?\s*件\s*成交', sales_str)
    if match:
        num = float(match.group(1))
        unit = match.group(2) or ""
        multipliers = {"万": 10000, "千": 1000, "百": 100}
        return int(num * multipliers.get(unit, 1))

    match = re.search(r'(\d+(?:\.\d+)?)\s*(万|千|百)\+?\s*件?', sales_str)
    if match:
        num = float(match.group(1))
        unit = match.group(2) or ""
        multipliers = {"万": 10000, "千": 1000, "百": 100}
        return int(num * multipliers.get(unit, 1))

    return 0


def get_offer_id(url: str) -> str:
    """从URL提取offer_id"""
    if not url:
        return ""
    match = re.search(r'offer/(\d+)', url)
    if match:
        return match.group(1)
    match = re.search(r'offerId=(\d+)', url)
    if match:
        return match.group(1)
    return hashlib.md5(url.encode()).hexdigest()[:16]


async def decrypt_link(page, encrypted_url: str) -> dict:
    """解密1688密文链接，获取真实链接和基本信息"""
    result = {"real_url": encrypted_url, "offer_id": ""}

    if "dj.1688.com/ci_bb" not in encrypted_url:
        # 已经是真实链接
        result["offer_id"] = get_offer_id(encrypted_url)
        return result

    try:
        # 访问加密链接，等待跳转
        resp = await page.goto(encrypted_url, timeout=30000, wait_until="domcontentloaded")
        await page.wait_for_timeout(2000)

        real_url = page.url
        result["real_url"] = real_url
        result["offer_id"] = get_offer_id(real_url)

        # 提取offer_id
        if "detail.1688.com" in real_url:
            match = re.search(r'offer/(\d+)\.html', real_url)
            if match:
                result["offer_id"] = match.group(1)

    except Exception as e:
        print(f"    解密链接失败: {str(e)[:50]}")

    return result


async def fetch_product_detail(page, offer_id: str, real_url: str) -> dict:
    """获取商品详情页信息"""
    detail = {
        "rating": 0,
        "rating_count": 0,
        "min_order": 999,
        "shop_years": 0,
        "shop_dsr": 0,
        "shop_main_cat": "",
        "ship_address": "",
        "supports_dy_distribution": False,
        "supports_privacy_ship": False,
        "images": [],
        "skus": [],  # SKU列表：[{name, price, stock}]
        "postage": 0,  # 邮费
        "postage_text": ""  # 邮费说明
    }

    if not real_url or "detail.1688.com" not in real_url:
        return detail

    try:
        await page.goto(real_url, timeout=30000, wait_until="domcontentloaded")
        await page.wait_for_timeout(2000)

        # 提取详情页数据（包含SKU和邮费）
        detail_data = await page.evaluate('''() => {
            const data = {
                rating: 0,
                rating_count: 0,
                min_order: 999,
                shop_years: 0,
                shop_dsr: 0,
                shop_main_cat: "",
                ship_address: "",
                supports_dy_distribution: false,
                supports_privacy_ship: false,
                images: [],
                skus: [],
                postage: 0,
                postage_text: ""
            };

            const pageText = document.body.innerText || "";

            // 商品评分
            const ratingMatch = pageText.match(/评分[：:]?\\s*(\\d+[.]?\\d*)/);
            if (ratingMatch) data.rating = parseFloat(ratingMatch[1]);

            // 评价数
            const reviewMatch = pageText.match(/(\\d+)\\s*条评价/);
            if (reviewMatch) data.rating_count = parseInt(reviewMatch[1]);

            // 起订量
            const minOrderMatch = pageText.match(/起订[量]?[：:]?\\s*(\\d+)\\s*件/);
            if (minOrderMatch) data.min_order = parseInt(minOrderMatch[1]);

            // 发货地
            const shipMatch = pageText.match(/发货地[：:]?\\s*([^\\n]+)/);
            if (shipMatch) data.ship_address = shipMatch[1].trim().substring(0, 20);

            // 邮费提取
            // 方式1：从页面文字匹配
            const postageMatch = pageText.match(/运费[：:]?\\s*¥?\\s*(\\d+\\.?\\d*)|快递[：:]?\\s*¥?\\s*(\\d+\\.?\\d*)/);
            if (postageMatch) {
                data.postage = parseFloat(postageMatch[1] || postageMatch[2] || 0);
                data.postage_text = postageMatch[0];
            }
            // 免运费
            if (pageText.includes('免运费') || pageText.includes('包邮') || pageText.includes('免邮')) {
                data.postage = 0;
                data.postage_text = "包邮/免运费";
            }

            // 从页面脚本提取数据
            const scripts = document.querySelectorAll('script');
            for (const script of scripts) {
                const text = script.textContent || '';

                // 尝试提取全局数据
                if (text.includes('globalData') || text.includes('window.__INITIAL_STATE__')) {
                    try {
                        // 商品评分
                        const rateMatch = text.match(/"star"[:]\\s*"?(\d+[.]?\d*)"?/);
                        if (rateMatch) data.rating = parseFloat(rateMatch[1]);

                        // 起订量
                        const beginNumMatch = text.match(/"beginNum"[:]\\s*(\\d+)/);
                        if (beginNumMatch) data.min_order = parseInt(beginNumMatch[1]);

                        // SKU信息提取 - 从skuMap或skuProps
                        // 格式1: skuMap
                        const skuMapMatch = text.match(/skuMap['"]*\\s*[:=]\\s*\\{([^}]+)\\}/);
                        if (skuMapMatch) {
                            try {
                                // 尝试提取SKU价格
                                const priceMatches = skuMapMatch[1].matchAll(/"price"\\s*:\\s*(\\d+\\.?\\d*)/g);
                                for (const m of priceMatches) {
                                    data.skus.push({name: '', price: parseFloat(m[1]), stock: 0});
                                }
                            } catch(e) {}
                        }

                        // 格式2: skuProps + skuList
                        if (text.includes('skuProps') || text.includes('skuList')) {
                            try {
                                // 提取SKU名称
                                const propNameMatch = text.matchAll(/"name"['"]*\\s*:\\s*['"]([^'"]+)['"]/g);
                                const propNames = [];
                                for (const m of propNameMatch) {
                                    if (m[1] && m[1].length < 50) propNames.push(m[1]);
                                }

                                // 提取SKU价格
                                const priceMatch = text.matchAll(/"price"\\s*:\\s*"?(\d+\.?\d*)"?/g);
                                const prices = [];
                                for (const m of priceMatch) {
                                    const p = parseFloat(m[1]);
                                    if (p > 0 && p < 100000 && !prices.includes(p)) {
                                        prices.push(p);
                                    }
                                }

                                // 组合SKU
                                if (prices.length > 0) {
                                    data.skus = prices.map((p, i) => ({
                                        name: propNames[i] || '',
                                        price: p,
                                        stock: 0
                                    }));
                                }
                            } catch(e) {}
                        }

                        // 邮费提取 - 从脚本
                        const postageScriptMatch = text.match(/"postage"\\s*:\\s*(\\d+\\.?\\d*)/);
                        if (postageScriptMatch && data.postage === 0) {
                            data.postage = parseFloat(postageScriptMatch[1]);
                        }
                        const freightMatch = text.match(/"freight"\\s*:\\s*(\\d+\\.?\\d*)/);
                        if (freightMatch) {
                            data.postage = parseFloat(freightMatch[1]);
                            data.postage_text = "运费: ¥" + data.postage;
                        }

                    } catch(e) {}
                }
            }

            // SKU信息提取 - 从DOM元素
            const skuItems = document.querySelectorAll('[class*="sku-item"], [class*="spec-item"], .sku-list li, .spec-list li');
            if (skuItems.length > 0 && data.skus.length === 0) {
                skuItems.forEach((item, idx) => {
                    const nameEl = item.querySelector('[class*="name"], [class*="title"]');
                    const priceEl = item.querySelector('[class*="price"]');
                    if (nameEl || priceEl) {
                        data.skus.push({
                            name: nameEl ? nameEl.innerText.trim().substring(0, 50) : '',
                            price: priceEl ? parseFloat(priceEl.innerText.replace(/[^0-9.]/g, '')) || 0 : 0,
                            stock: 0
                        });
                    }
                });
            }

            // 价格区间提取（作为SKU的备选）
            if (data.skus.length === 0) {
                const priceRangeEl = document.querySelector('[class*="price-original"], [class*="price-box"], .price-container');
                if (priceRangeEl) {
                    const priceText = priceRangeEl.innerText || '';
                    const prices = priceText.match(/\\d+\\.?\\d*/g);
                    if (prices && prices.length > 0) {
                        prices.forEach(p => {
                            const price = parseFloat(p);
                            if (price > 0 && price < 100000) {
                                data.skus.push({name: '默认', price: price, stock: 0});
                            }
                        });
                    }
                }
            }

            // 邮费提取 - 从DOM
            if (data.postage === 0 && data.postage_text === '') {
                const freightEl = document.querySelector('[class*="freight"], [class*="postage"], [class*="shipping"]');
                if (freightEl) {
                    const freightText = freightEl.innerText || '';
                    const freightMatch = freightText.match(/(\\d+\\.?\\d*)/);
                    if (freightMatch) {
                        data.postage = parseFloat(freightMatch[1]);
                        data.postage_text = freightText.trim().substring(0, 30);
                    } else if (freightText.includes('免') || freightText.includes('包')) {
                        data.postage = 0;
                        data.postage_text = "包邮";
                    }
                }
            }

            // 店铺信息
            const shopInfo = document.querySelector('.shop-info, .supplier-info, [class*="shop"]');
            if (shopInfo) {
                const shopText = shopInfo.innerText || '';

                // 店铺年限
                const yearMatch = shopText.match(/(\\d+)\\s*年/);
                if (yearMatch) data.shop_years = parseInt(yearMatch[1]);

                // DSR评分
                const dsrMatch = shopText.match(/DSR[：:]?\\s*(\\d+[.]?\\d*)/);
                if (dsrMatch) data.shop_dsr = parseFloat(dsrMatch[1]);
            }

            // 店铺主营类目
            const catEl = document.querySelector('[class*="category"], [class*="主营"]');
            if (catEl) data.shop_main_cat = catEl.innerText.trim().substring(0, 30);

            // 检测是否支持抖音分销
            if (pageText.includes('抖音') || pageText.includes('抖店') ||
                pageText.includes('分销') || document.querySelector('[class*="distribution"]')) {
                data.supports_dy_distribution = true;
            }

            // 检测是否支持密文发货
            if (pageText.includes('隐私') || pageText.includes('密文') ||
                pageText.includes('保密发货')) {
                data.supports_privacy_ship = true;
            }

            // 商品图片
            const imgEls = document.querySelectorAll('.detail-gallery img, [class*="main-image"] img');
            imgEls.forEach((img, idx) => {
                if (idx < 3 && img.src) {
                    data.images.push(img.src);
                }
            });

            return data;
        }''')

        if detail_data:
            detail.update(detail_data)

    except Exception as e:
        print(f"    详情页获取失败: {str(e)[:50]}")

    return detail


async def search_1688(keyword: str, min_count: int, page, fetch_detail: bool = True):
    """搜索1688商品（完整版）"""
    results = []
    seen_offer_ids = set()  # 去重

    search_url = f"https://www.1688.com/zw/page.html?hpageId=old-sem-pc-list&keywords={quote(keyword)}"

    print(f"\n{'='*60}")
    print(f"搜索: {keyword}")
    print(f"目标: {min_count}个")
    print(f"{'='*60}")

    try:
        print("加载搜索页...")
        await page.goto(search_url, timeout=90000)
        await page.wait_for_timeout(4000)

        # 滚动加载
        for i in range(5):
            await page.evaluate(f"window.scrollBy(0, {(i+1)*600})")
            await page.wait_for_timeout(400)
        await page.wait_for_timeout(2000)

        print("提取商品列表...")
        products = await page.evaluate('''() => {
            const results = [];
            const allLinks = document.querySelectorAll('a[href*="dj.1688.com/ci_bb"], a[href*="offer/"]');
            const seen = new Set();

            allLinks.forEach(link => {
                const href = link.href;
                if (seen.has(href) || href.includes('javascript:')) return;
                seen.add(href);

                let title = link.title || link.getAttribute('title') || '';
                if (!title) title = link.innerText || '';
                title = title.trim();
                if (title.length < 5) return;

                let container = link.closest('[class*="item"]') || link.closest('li') ||
                               link.parentElement?.parentElement?.parentElement;
                if (!container) container = link.parentElement;
                if (!container) return;

                const containerText = container.innerText || '';

                // 价格
                let priceText = '';
                const priceEl = container.querySelector('.offer-price, .price, [class*="price"]');
                if (priceEl) priceText = priceEl.innerText;
                if (!priceText) {
                    const pm = containerText.match(/[¥￥]?\\s*(\\d+\\.?\\d*)/);
                    if (pm) priceText = pm[0];
                }

                // 销量
                let salesNum = 0;
                let salesText = '';
                const salesMatch = containerText.match(/成交\\s*(\\d+(?:\\.\\d+)?)\\s*(万|千|百)?\\+?\\s*件/);
                if (salesMatch) {
                    const num = parseFloat(salesMatch[1]);
                    const unit = salesMatch[2] || '';
                    const mult = {'万': 10000, '千': 1000, '百': 100};
                    salesNum = num * (mult[unit] || 1);
                    salesText = salesMatch[0];
                }

                // 标签
                const tags = [];
                if (containerText.includes('一件代发') || containerText.includes('代发')) tags.push('一件代发');
                if (containerText.includes('3日达') || containerText.includes('三日达')) tags.push('3日达');
                if (containerText.includes('48小时') || containerText.includes('24小时')) tags.push('快速发货');
                if (containerText.includes('运费险')) tags.push('运费险');
                if (containerText.includes('七天无理由') || containerText.includes('7天无理由')) tags.push('七天无理由');
                if (containerText.includes('分销') || containerText.includes('铺货')) tags.push('支持分销');

                // 回头率
                let repeatRate = 0;
                const repeatMatch = containerText.match(/回头率\\s*[：:]?\\s*(\\d+(?:\\.\\d+)?)\\s*%?/);
                if (repeatMatch) repeatRate = parseFloat(repeatMatch[1]);

                // 店铺名
                let shopText = '';
                const shopEl = container.querySelector('[class*="shop"], [class*="company"]');
                if (shopEl) shopText = shopEl.innerText.trim();

                results.push({
                    name: title.substring(0, 100),
                    link: href,
                    price_raw: priceText.trim(),
                    sales: salesNum,
                    sales_raw: salesText,
                    shop_name: shopText,
                    tags: tags,
                    repeat_rate: repeatRate
                });
            });

            return results;
        }''')

        print(f"找到 {len(products) if products else 0} 个商品")

        skipped = {"price": 0, "sales": 0, "dropship": 0, "delivery": 0, "duplicate": 0}

        if products:
            for p in products:
                if len(results) >= min_count:
                    break

                if not p.get("name") or not p.get("link"):
                    continue

                # 解析价格
                price = parse_price(p.get("price_raw", ""))

                # 价格筛选
                if price <= 15 or price > 300:
                    skipped["price"] += 1
                    continue

                # 解析销量
                sales = p.get("sales", 0)
                if isinstance(sales, str):
                    sales = parse_sales(sales)

                # 销量筛选
                if sales < 500:
                    skipped["sales"] += 1
                    continue

                # 标签筛选
                tags = p.get("tags", [])
                if "一件代发" not in tags:
                    skipped["dropship"] += 1
                    continue

                has_fast_delivery = "3日达" in tags or "快速发货" in tags
                if not has_fast_delivery:
                    skipped["delivery"] += 1
                    continue

                # 解密链接获取真实链接
                print(f"  解密链接: {p['name'][:30]}...")
                link_info = await decrypt_link(page, p["link"])
                real_url = link_info["real_url"]
                offer_id = link_info["offer_id"]

                # 去重检查
                if offer_id and offer_id in seen_offer_ids:
                    skipped["duplicate"] += 1
                    print(f"    [跳过-重复] {offer_id}")
                    continue
                if offer_id:
                    seen_offer_ids.add(offer_id)

                # 获取详情页信息
                detail = {"rating": 0, "min_order": 999, "shop_years": 0, "ship_address": "",
                         "supports_dy_distribution": False, "supports_privacy_ship": False,
                         "skus": [], "postage": 0, "postage_text": ""}

                if fetch_detail and real_url:
                    print(f"  获取详情: {p['name'][:30]}...")
                    detail = await fetch_product_detail(page, offer_id, real_url)

                    # 详情页条件筛选
                    # 起订量 > 2 跳过
                    if detail["min_order"] > 2:
                        print(f"    [跳过-起订量{detail['min_order']}件]")
                        continue

                    # 邮费 > 5 跳过
                    postage = detail.get("postage", 0)
                    if postage > 5:
                        print(f"    [跳过-邮费¥{postage}]")
                        continue

                    # 更新标签
                    if detail["supports_dy_distribution"]:
                        tags.append("抖店分销")
                    if detail["supports_privacy_ship"]:
                        tags.append("密文发货")

                # 计算评分
                score = 0
                score += sales / 100
                score += min(price * 0.3, 50)

                if "运费险" in tags:
                    score += 20
                if "七天无理由" in tags:
                    score += 15
                if "支持分销" in tags or "抖店分销" in tags:
                    score += 25
                if "密文发货" in tags:
                    score += 15

                repeat_rate = p.get("repeat_rate", 0)
                if repeat_rate >= 30:
                    score += 30
                elif repeat_rate >= 20:
                    score += 20
                elif repeat_rate >= 10:
                    score += 10

                if detail["rating"] >= 4.8:
                    score += 20
                elif detail["rating"] >= 4.5:
                    score += 10

                if detail["shop_years"] >= 5:
                    score += 15
                elif detail["shop_years"] >= 3:
                    score += 10

                product = {
                    "name": p["name"],
                    "link": p["link"],
                    "real_link": real_url,
                    "offer_id": offer_id,
                    "price_raw": p.get("price_raw", ""),
                    "price": price,
                    "sales": sales,
                    "sales_raw": p.get("sales_raw", f"{sales}件"),
                    "shop_name": p.get("shop_name", ""),
                    "tags": tags,
                    "repeat_rate": repeat_rate,
                    "rating": detail["rating"],
                    "min_order": detail["min_order"],
                    "shop_years": detail["shop_years"],
                    "ship_address": detail["ship_address"],
                    "supports_dy": detail["supports_dy_distribution"],
                    "supports_privacy": detail["supports_privacy_ship"],
                    "skus": detail.get("skus", []),
                    "postage": detail.get("postage", 0),
                    "postage_text": detail.get("postage_text", ""),
                    "score": round(score, 2),
                    "keyword": keyword
                }

                results.append(product)
                print(f"\n[{len(results)}] {product['name'][:45]}")
                print(f"    价格: ¥{product['price']} | 销量: {product['sales']} | 评分: {product['score']}")
                print(f"    标签: {', '.join(tags)}")
                # SKU价格信息
                if detail.get("skus"):
                    sku_prices = [f"¥{s['price']}" for s in detail["skus"][:5] if s.get("price", 0) > 0]
                    if sku_prices:
                        print(f"    SKU价格: {', '.join(sku_prices)}")
                # 邮费信息
                if detail.get("postage_text"):
                    print(f"    邮费: {detail['postage_text']}")
                elif detail.get("postage", 0) > 0:
                    print(f"    邮费: ¥{detail['postage']}")
                else:
                    print(f"    邮费: 包邮/免运费")
                if detail["rating"] > 0:
                    print(f"    商品评分: {detail['rating']} | 起订: {detail['min_order']}件")
                if detail["shop_years"] > 0:
                    print(f"    店铺年限: {detail['shop_years']}年 | 发货地: {detail['ship_address']}")

        print(f"\n跳过统计: 价格={skipped['price']}, 销量={skipped['sales']}, " +
              f"无代发={skipped['dropship']}, 无快发={skipped['delivery']}, 重复={skipped['duplicate']}")

    except Exception as e:
        print(f"错误: {e}")
        import traceback
        traceback.print_exc()

    return results


def save_to_json(data: dict, filename: str):
    """保存为JSON"""
    with open(filename, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"保存JSON: {filename}")


def save_to_excel(products: list, filename: str):
    """保存为Excel格式CSV（兼容）"""
    import csv

    headers = ["序号", "商品名称", "价格(元)", "SKU价格", "邮费", "销量", "评分", "商品评分", "起订量",
               "店铺名称", "店铺年限", "发货地", "回头率%", "标签", "真实链接", "原始链接"]

    with open(filename, "w", encoding="utf-8-sig", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(headers)

        for i, p in enumerate(products, 1):
            # SKU价格格式化
            skus = p.get("skus", [])
            if skus:
                sku_prices = [f"{s.get('name', '')}¥{s.get('price', 0)}" for s in skus if s.get('price', 0) > 0]
                sku_str = "; ".join(sku_prices[:10])  # 最多10个SKU
            else:
                sku_str = f"¥{p.get('price', 0)}"

            # 邮费格式化
            postage = p.get("postage", 0)
            postage_text = p.get("postage_text", "")
            if postage_text:
                postage_str = postage_text
            elif postage > 0:
                postage_str = f"¥{postage}"
            else:
                postage_str = "包邮"

            row = [
                i,
                p.get("name", ""),
                p.get("price", 0),
                sku_str,
                postage_str,
                p.get("sales", 0),
                p.get("score", 0),
                p.get("rating", 0),
                p.get("min_order", 0),
                p.get("shop_name", ""),
                p.get("shop_years", 0),
                p.get("ship_address", ""),
                p.get("repeat_rate", 0),
                ", ".join(p.get("tags", [])),
                p.get("real_link", ""),
                p.get("link", "")
            ]
            writer.writerow(row)

    print(f"保存Excel: {filename}")


async def run_filter(keywords: list, count: int, output_format: str = "json"):
    """执行筛选"""
    all_results = []
    seen_global = set()  # 全局去重

    async with async_playwright() as p:
        browser = await p.chromium.launch(
            headless=False,
            args=['--disable-blink-features=AutomationControlled']
        )

        context = await browser.new_context(
            viewport={"width": 1400, "height": 900},
            user_agent="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"
        )

        await context.add_init_script("Object.defineProperty(navigator, 'webdriver', {get: () => false})")
        page = await context.new_page()

        for kw in keywords:
            per_kw = max(count // len(keywords), 3)
            results = await search_1688(kw, per_kw, page, fetch_detail=True)

            # 全局去重
            for r in results:
                if r["offer_id"] and r["offer_id"] not in seen_global:
                    seen_global.add(r["offer_id"])
                    all_results.append(r)

        await browser.close()

    # 按评分排序
    all_results.sort(key=lambda x: x.get("score", 0), reverse=True)
    return all_results[:count]


def main():
    parser = argparse.ArgumentParser(description="1688商品筛选（完整版）")
    parser.add_argument("--keywords", required=True, help="关键词，逗号分隔")
    parser.add_argument("--count", type=int, default=10, help="数量")
    parser.add_argument("--output", choices=["json", "excel", "both"], default="both", help="输出格式")

    args = parser.parse_args()
    keywords = [k.strip() for k in args.keywords.split(",") if k.strip()]

    print(f"关键词: {keywords}")
    print(f"目标: {args.count}个")
    print(f"筛选条件:")
    print(f"  - 价格: 15-300元")
    print(f"  - 销量: >500")
    print(f"  - 必须一件代发 + 快速发货")
    print(f"  - 起订量: ≤2件")
    print(f"  - 邮费: ≤5元")
    print(f"  - 加分: 运费险、七天无理由、抖店分销、密文发货")
    print("-" * 60)

    start = time.time()
    results = asyncio.run(run_filter(keywords, args.count, args.output))

    print(f"\n{'='*60}")
    print(f"完成! 共 {len(results)} 个商品, 耗时 {time.time()-start:.1f}秒")

    # 保存结果
    ts = time.strftime("%Y%m%d_%H%M%S")

    # 确保输出目录存在
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    if args.output in ["json", "both"]:
        json_file = os.path.join(OUTPUT_DIR, f"1688_优质商品_{ts}.json")
        save_to_json({
            "time": time.strftime("%Y-%m-%d %H:%M:%S"),
            "keywords": keywords,
            "conditions": {
                "min_price": 15, "max_price": 300,
                "min_sales": 500, "max_min_order": 2, "max_postage": 5,
                "required": ["一件代发", "快速发货"],
                "bonus": ["运费险", "七天无理由", "抖店分销", "密文发货"]
            },
            "total": len(results),
            "products": results
        }, json_file)

    if args.output in ["excel", "both"]:
        excel_file = os.path.join(OUTPUT_DIR, f"1688_优质商品_{ts}.csv")
        save_to_excel(results, excel_file)

    # 打印结果
    for i, p in enumerate(results, 1):
        print(f"\n{i}. {p['name']}")
        print(f"   价格: ¥{p['price']} | 销量: {p['sales']} | 评分: {p['score']}")
        # SKU价格
        skus = p.get("skus", [])
        if skus:
            sku_prices = [f"¥{s.get('price', 0)}" for s in skus[:5] if s.get('price', 0) > 0]
            if sku_prices:
                print(f"   SKU价格: {', '.join(sku_prices)}")
        # 邮费
        postage = p.get("postage", 0)
        postage_text = p.get("postage_text", "")
        if postage_text:
            print(f"   邮费: {postage_text}")
        elif postage > 0:
            print(f"   邮费: ¥{postage}")
        else:
            print(f"   邮费: 包邮")
        print(f"   标签: {', '.join(p.get('tags', []))}")
        if p.get('rating', 0) > 0:
            print(f"   商品评分: {p['rating']} | 起订: {p.get('min_order', 0)}件")
        if p.get('shop_years', 0) > 0:
            print(f"   店铺: {p.get('shop_years', 0)}年 | {p.get('shop_name', '')}")
        print(f"   真实链接: {p.get('real_link', p['link'])}")


if __name__ == "__main__":
    main()