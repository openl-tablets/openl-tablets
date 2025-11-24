#!/bin/bash

echo "=========================================="
echo "Получение Refresh Token для Ping Identity"
echo "=========================================="
echo ""

# Configuration
CLIENT_ID="${OPENL_OAUTH2_CLIENT_ID:-OpenL_Studio}"
AUTHORIZATION_URL="${OPENL_OAUTH2_AUTHORIZATION_URL:-https://testping-sso.eisgroup.com/as/authorization.oauth2}"
REDIRECT_URI="${OPENL_OAUTH2_REDIRECT_URI:-https://dc3eisovpn03-fast.eisgroup.com/oauth2/callback}"
SCOPE="${OPENL_OAUTH2_SCOPE:-openid profile email}"

# Build authorization URL
AUTH_URL="${AUTHORIZATION_URL}?response_type=code&client_id=${CLIENT_ID}&scope=$(echo -n "$SCOPE" | jq -sRr @uri 2>/dev/null || echo "$SCOPE" | sed 's/ /%20/g')&redirect_uri=$(echo -n "$REDIRECT_URI" | jq -sRr @uri 2>/dev/null || echo "$REDIRECT_URI" | sed 's/:/%3A/g; s/\//%2F/g')"

echo "1. Открываю браузер с URL авторизации..."
echo ""

# Function to open browser based on OS
open_browser() {
  local url="$1"
  if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    open "$url"
  elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    if command -v xdg-open &> /dev/null; then
      xdg-open "$url"
    elif command -v gnome-open &> /dev/null; then
      gnome-open "$url"
    else
      echo "⚠️  Не удалось автоматически открыть браузер. Установите xdg-open или откройте URL вручную:"
      echo "$url"
      return 1
    fi
  elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
    # Windows
    start "$url"
  else
    echo "⚠️  Неизвестная ОС. Откройте URL вручную:"
    echo "$url"
    return 1
  fi
  return 0
}

# Open browser automatically
if open_browser "$AUTH_URL"; then
  echo "✅ Браузер открыт с URL авторизации"
else
  echo "URL для ручного открытия:"
  echo "$AUTH_URL"
fi

echo ""
echo "2. Войдите с вашими учетными данными"
echo ""
echo "3. После входа вы будете перенаправлены с параметром 'code' в URL"
echo "   Скопируйте ПОЛНЫЙ URL из адресной строки браузера"
echo ""
read -p "Вставьте полный URL с параметром code: " FULL_URL

# Extract authorization code from URL (macOS compatible - doesn't use -P flag)
AUTH_CODE=$(echo "$FULL_URL" | sed -n 's/.*[?&]code=\([^&]*\).*/\1/p' | head -1)
if [ -z "$AUTH_CODE" ]; then
  # Fallback: try to extract from URL directly
  AUTH_CODE=$(echo "$FULL_URL" | sed 's/.*code=\([^&]*\).*/\1/')
fi

# Extract redirect_uri from URL (if present)
EXTRACTED_REDIRECT_URI=$(echo "$FULL_URL" | sed -n 's/.*[?&]redirect_uri=\([^&]*\).*/\1/p' | head -1)
if [ -n "$EXTRACTED_REDIRECT_URI" ]; then
  # Use redirect_uri from URL if present
  REDIRECT_URI="$EXTRACTED_REDIRECT_URI"
fi

if [ -z "$AUTH_CODE" ]; then
  echo ""
  echo "Не удалось извлечь authorization code из URL"
  echo "Попробуйте ввести код вручную:"
  read -p "Введите authorization code: " AUTH_CODE
fi

if [ -z "$AUTH_CODE" ]; then
  echo "❌ Authorization code не может быть пустым"
  exit 1
fi

echo ""
echo "Обмениваю authorization code на токены..."

echo ""
echo "Используемые параметры:"
echo "  Authorization code: ${AUTH_CODE:0:20}..."
echo "  Redirect URI: $REDIRECT_URI"
echo ""

# Try with Basic Auth first
RESPONSE=$(curl -s -X POST https://testping-sso.eisgroup.com/as/token.oauth2 \
  -H "Authorization: Basic $(echo -n 'OpenL_Studio:Exigen/2024.02' | base64)" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code&code=$AUTH_CODE&redirect_uri=$REDIRECT_URI")

# If that fails, try with client_secret_post
if echo "$RESPONSE" | grep -q "error"; then
  echo "Пробую с client_secret_post вместо Basic Auth..."
  RESPONSE=$(curl -s -X POST https://testping-sso.eisgroup.com/as/token.oauth2 \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=authorization_code&code=$AUTH_CODE&redirect_uri=$REDIRECT_URI&client_id=OpenL_Studio&client_secret=Exigen/2024.02")
fi

echo ""
echo "Ответ сервера:"
echo "$RESPONSE" | jq . 2>/dev/null || echo "$RESPONSE"

REFRESH_TOKEN=$(echo "$RESPONSE" | jq -r '.refresh_token' 2>/dev/null)

if [ "$REFRESH_TOKEN" != "null" ] && [ -n "$REFRESH_TOKEN" ] && [ "$REFRESH_TOKEN" != "" ]; then
  echo ""
  echo "=========================================="
  echo "✅ Refresh Token успешно получен!"
  echo "=========================================="
  echo ""
  echo "Добавьте следующие строки в compose.yaml (секция mcp-server environment):"
  echo ""
  echo "  OPENL_OAUTH2_REFRESH_TOKEN: \"$REFRESH_TOKEN\""
  echo "  OPENL_OAUTH2_GRANT_TYPE: refresh_token"
  echo ""
  echo "И закомментируйте или удалите:"
  echo "  # OPENL_OAUTH2_USE_BASIC_AUTH: \"true\""
  echo ""
else
  echo ""
  echo "=========================================="
  echo "❌ Не удалось получить refresh_token"
  echo "=========================================="
  echo ""
  echo "Проверьте:"
  echo "1. Authorization code правильный и не истек"
  echo "2. Redirect URI совпадает: http://localhost:8080/login/oauth2/code/webstudio"
  echo "3. Клиент настроен для authorization_code grant type"
  exit 1
fi

