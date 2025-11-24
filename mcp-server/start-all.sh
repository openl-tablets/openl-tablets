#!/bin/bash

# Скрипт для запуска OpenL Tablets и проверки MCP-сервера

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🚀 Запуск OpenL Tablets и проверка MCP-сервера${NC}"
echo "=========================================="
echo ""

# Проверка Docker
if command -v docker &> /dev/null && docker ps &> /dev/null; then
    echo -e "${GREEN}✓${NC} Docker доступен"
    
    # Проверка, запущен ли уже OpenL
    if docker ps | grep -q "studio\|postgres"; then
        echo -e "${YELLOW}⚠${NC}  OpenL контейнеры уже запущены"
        echo ""
        echo "Выберите действие:"
        echo "  1) Использовать уже запущенные контейнеры"
        echo "  2) Перезапустить контейнеры"
        echo "  3) Выход"
        read -p "Ваш выбор (1-3): " choice
        
        case $choice in
            2)
                echo "Останавливаем контейнеры..."
                cd /Users/asamuseu/IdeaProjects/openl-tablets
                docker compose down
                echo "Запускаем заново..."
                docker compose up -d
                ;;
            3)
                exit 0
                ;;
            *)
                echo "Используем существующие контейнеры"
                ;;
        esac
    else
        echo "Запускаем OpenL Tablets через Docker Compose..."
        cd /Users/asamuseu/IdeaProjects/openl-tablets
        docker compose up -d
        
        echo ""
        echo -e "${YELLOW}⏳ Ожидание запуска сервисов (это займёт 1-2 минуты)...${NC}"
        sleep 5
        
        # Ждём, пока сервисы станут доступны
        for i in {1..30}; do
            if curl -s http://localhost:8080 > /dev/null 2>&1; then
                echo -e "${GREEN}✓${NC} OpenL Tablets запущен!"
                break
            fi
            echo -n "."
            sleep 2
        done
        echo ""
    fi
else
    echo -e "${YELLOW}⚠${NC}  Docker не найден или не запущен"
    echo ""
    echo "Альтернатива: запустите OpenL локально через:"
    echo "  cd /Users/asamuseu/IdeaProjects/openl-tablets/DEMO"
    echo "  ./start"
    echo ""
    read -p "Нажмите Enter, чтобы продолжить проверку MCP-сервера..."
fi

echo ""
echo "=========================================="
echo -e "${BLUE}🔍 Проверка MCP-сервера${NC}"
echo "=========================================="
echo ""

cd /Users/asamuseu/IdeaProjects/openl-tablets/mcp-server

# Установка переменных окружения для проверки
export OPENL_BASE_URL="http://localhost:8080/rest"
export OPENL_USERNAME="admin"
export OPENL_PASSWORD="admin"

# Запуск проверки
if [ -f "./check-health.sh" ]; then
    ./check-health.sh
else
    echo "Скрипт проверки не найден. Выполняем базовые проверки..."
    
    # Проверка сборки
    if [ -f "dist/index.js" ]; then
        echo -e "${GREEN}✓${NC} MCP-сервер собран"
    else
        echo "Собираем MCP-сервер..."
        npm run build
    fi
    
    # Проверка конфигурации Claude Desktop
    CLAUDE_CONFIG="$HOME/Library/Application Support/Claude/config.json"
    if [ -f "$CLAUDE_CONFIG" ] && grep -q "openl-mcp-server" "$CLAUDE_CONFIG" 2>/dev/null; then
        echo -e "${GREEN}✓${NC} MCP-сервер настроен в Claude Desktop"
    else
        echo -e "${YELLOW}⚠${NC}  MCP-сервер не найден в конфигурации Claude Desktop"
    fi
fi

echo ""
echo "=========================================="
echo -e "${GREEN}✅ Готово!${NC}"
echo "=========================================="
echo ""
echo "Следующие шаги:"
echo ""
echo "1. Откройте в браузере: ${BLUE}http://localhost:8080${NC}"
echo "   Войдите: admin / admin"
echo ""
echo "2. Перезапустите Claude Desktop (полностью закройте и откройте снова)"
echo ""
echo "3. В Claude Desktop:"
echo "   - Откройте настройки → MCP Servers"
echo "   - Убедитесь, что openl-mcp-server подключен"
echo ""
echo "4. В чате с Claude попробуйте:"
echo "   ${BLUE}Список репозиториев в OpenL Tablets${NC}"
echo ""
echo "Для остановки Docker контейнеров:"
echo "  cd /Users/asamuseu/IdeaProjects/openl-tablets"
echo "  docker compose down"
echo ""

