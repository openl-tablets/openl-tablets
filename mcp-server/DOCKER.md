# 🐳 Docker и Docker Compose для MCP Server

## Обзор

MCP Server теперь может работать как отдельное HTTP-приложение на Express, что позволяет интегрировать его в Docker Compose как микросервис.

## Архитектура

```
┌─────────────────┐
│   HTTP Client   │  ← Внешние запросы
└────────┬────────┘
         │ HTTP REST API
         │
         ▼
┌─────────────────┐
│ Express Server  │  ← HTTP API на порту 3000
│  (server.ts)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  MCP Tools      │  ← Вызов инструментов OpenL
│  (tool-handlers)│
└────────┬────────┘
         │ HTTP REST API
         │
         ▼
┌─────────────────┐
│  OpenL Studio   │  ← OpenL Tablets API
│  (studio:8080)  │
└─────────────────┘
```

## Запуск через Docker Compose

### Быстрый старт

```bash
cd /Users/asamuseu/IdeaProjects/openl-tablets
docker compose up mcp-server
```

### Запуск всего стека

```bash
docker compose up
```

Это запустит:
- PostgreSQL
- OpenL Studio (порт 8080)
- Rule Services (порт 8081)
- MCP Server (порт 3000)
- Nginx Proxy (порт 80)

## HTTP API Endpoints

### Health Check
```bash
GET http://localhost:3000/health
```

Ответ:
```json
{
  "status": "ok",
  "timestamp": "2024-01-01T12:00:00.000Z",
  "service": "openl-mcp-server",
  "version": "1.0.0"
}
```

### Список инструментов
```bash
GET http://localhost:3000/tools
```

Ответ:
```json
{
  "tools": [
    {
      "name": "openl_list_repositories",
      "title": "openl List Repositories",
      "description": "...",
      "inputSchema": {...}
    },
    ...
  ],
  "count": 18
}
```

### Информация об инструменте
```bash
GET http://localhost:3000/tools/openl_list_repositories
```

### Выполнение инструмента

**Вариант 1: Через endpoint инструмента**
```bash
POST http://localhost:3000/tools/openl_list_repositories/execute
Content-Type: application/json

{
  "repository": "design"
}
```

**Вариант 2: Универсальный endpoint**
```bash
POST http://localhost:3000/execute
Content-Type: application/json

{
  "tool": "openl_list_repositories",
  "arguments": {
    "repository": "design"
  }
}
```

## Переменные окружения

MCP Server использует следующие переменные окружения:

| Переменная | Описание | По умолчанию |
|------------|----------|--------------|
| `PORT` | Порт HTTP сервера | `3000` |
| `OPENL_BASE_URL` | URL OpenL Studio API | `http://studio:8080/rest` |
| `OPENL_USERNAME` | Имя пользователя | `admin` |
| `OPENL_PASSWORD` | Пароль | `admin` |
| `OPENL_CLIENT_DOCUMENT_ID` | ID клиента для трекинга | `docker-compose-1` |
| `NODE_ENV` | Режим работы | `production` |

## Доступ через Nginx Proxy

MCP Server также доступен через Nginx proxy:

```bash
# Health check
GET http://localhost/mcp/health

# Список инструментов
GET http://localhost/mcp/tools

# Выполнение инструмента
POST http://localhost/mcp/execute
```

## Локальная разработка

### Запуск без Docker

```bash
cd mcp-server

# Установка зависимостей
npm install

# Сборка
npm run build

# Запуск HTTP сервера
export OPENL_BASE_URL="http://localhost:8080/rest"
export OPENL_USERNAME="admin"
export OPENL_PASSWORD="admin"
npm run start:http
```

### Режим разработки с автопересборкой

```bash
# Терминал 1: Автопересборка
npm run watch

# Терминал 2: Запуск сервера
npm run dev:http
```

## Тестирование

### Проверка здоровья
```bash
curl http://localhost:3000/health
```

### Список инструментов
```bash
curl http://localhost:3000/tools | jq
```

### Выполнение инструмента
```bash
curl -X POST http://localhost:3000/execute \
  -H "Content-Type: application/json" \
  -d '{
    "tool": "openl_list_repositories",
    "arguments": {}
  }' | jq
```

## Логи

### Просмотр логов Docker Compose
```bash
docker compose logs -f mcp-server
```

### Логи в реальном времени
```bash
docker compose logs -f --tail=100 mcp-server
```

## Troubleshooting

### MCP Server не запускается

1. Проверьте логи:
   ```bash
   docker compose logs mcp-server
   ```

2. Проверьте, что OpenL Studio запущен:
   ```bash
   docker compose ps studio
   ```

3. Проверьте переменные окружения:
   ```bash
   docker compose exec mcp-server env | grep OPENL
   ```

### Ошибка подключения к OpenL

Убедитесь, что:
- OpenL Studio запущен (`docker compose ps studio`)
- `OPENL_BASE_URL` указывает на правильный адрес (`http://studio:8080/rest`)
- Учетные данные правильные (`OPENL_USERNAME`, `OPENL_PASSWORD`)

### Порт 3000 занят

Измените порт в `compose.yaml`:
```yaml
ports:
  - "3001:3000"  # Внешний:Внутренний
```

Или установите переменную окружения:
```yaml
environment:
  PORT: 3001
```

## Production Deployment

Для production рекомендуется:

1. Использовать HTTPS через reverse proxy
2. Настроить аутентификацию на уровне API
3. Ограничить ресурсы в `deploy.resources`
4. Настроить мониторинг и логирование
5. Использовать секреты для паролей (не хранить в compose.yaml)

## Примеры использования

### Python
```python
import requests

# Health check
response = requests.get('http://localhost:3000/health')
print(response.json())

# Выполнение инструмента
response = requests.post(
    'http://localhost:3000/execute',
    json={
        'tool': 'openl_list_repositories',
        'arguments': {}
    }
)
print(response.json())
```

### JavaScript/Node.js
```javascript
const axios = require('axios');

// Health check
const health = await axios.get('http://localhost:3000/health');
console.log(health.data);

// Выполнение инструмента
const result = await axios.post('http://localhost:3000/execute', {
  tool: 'openl_list_repositories',
  arguments: {}
});
console.log(result.data);
```

### cURL
```bash
# Health check
curl http://localhost:3000/health

# Список инструментов
curl http://localhost:3000/tools

# Выполнение инструмента
curl -X POST http://localhost:3000/execute \
  -H "Content-Type: application/json" \
  -d '{
    "tool": "openl_list_repositories",
    "arguments": {}
  }'
```

