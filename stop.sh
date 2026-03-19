#!/bin/bash

# 工作流项目停止脚本

echo "停止工作流服务..."

# 停止后端
pkill -f "spring-boot:run" 2>/dev/null

# 停止前端
pkill -f "vite" 2>/dev/null

echo "服务已停止"