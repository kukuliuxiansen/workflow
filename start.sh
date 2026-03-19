#!/bin/bash

# 工作流项目启动脚本

cd "$(dirname "$0")"

echo "=========================================="
echo "  Workflow Engine - 启动脚本"
echo "=========================================="

# 检查 Java
if ! command -v java &> /dev/null; then
    echo "错误: 未找到 Java，请先安装 JDK 8+"
    exit 1
fi

# 检查 Maven
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到 Maven，请先安装 Maven"
    exit 1
fi

# 创建必要的目录
mkdir -p data logs executions workflows

# 启动后端（直接在当前目录运行）
echo "启动后端服务..."
mvn spring-boot:run -DskipTests &
BACKEND_PID=$!

# 等待后端启动
echo "等待后端服务启动..."
sleep 15

# 检查后端是否启动
if curl -s http://localhost:3001/api/workflows > /dev/null 2>&1; then
    echo "后端服务启动成功 ✓"
else
    echo "等待后端启动中..."
    sleep 5
fi

# 检查前端目录
if [ -d "frontend" ]; then
    # 检查 Node.js
    if command -v npm &> /dev/null; then
        echo "启动前端服务..."
        cd frontend

        # 安装依赖
        if [ ! -d "node_modules" ]; then
            echo "安装前端依赖..."
            npm install
        fi

        # 启动前端
        npm run dev &
        FRONTEND_PID=$!
        cd ..

        echo ""
        echo "=========================================="
        echo "  服务已启动"
        echo "  后端: http://localhost:3001"
        echo "  前端: http://localhost:3000"
        echo "=========================================="
        echo ""
        echo "后端 PID: $BACKEND_PID"
        echo "前端 PID: $FRONTEND_PID"
        echo ""
        echo "按 Ctrl+C 停止服务"

        # 等待子进程
        wait
    else
        echo ""
        echo "=========================================="
        echo "  后端已启动"
        echo "  API: http://localhost:3001"
        echo "=========================================="
        echo ""
        echo "未找到 npm，跳过前端启动"
        echo "后端 PID: $BACKEND_PID"

        wait $BACKEND_PID
    fi
else
    echo ""
    echo "=========================================="
    echo "  后端已启动"
    echo "  API: http://localhost:3001"
    echo "=========================================="
    echo ""
    echo "前端目录不存在"
    echo "后端 PID: $BACKEND_PID"

    wait $BACKEND_PID
fi