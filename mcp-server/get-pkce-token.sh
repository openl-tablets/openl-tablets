#!/bin/bash

echo "=========================================="
echo "Получение Authorization Code с PKCE для Ping Identity"
echo "=========================================="
echo ""

# Generate code_verifier
echo "1. Генерирую code_verifier..."
CODE_VERIFIER=$(node -e "const crypto = require('crypto'); const charset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~'; const random = crypto.randomBytes(128); let result = ''; for (let i = 0; i < 128; i++) { result += charset[random[i] % charset.length]; } console.log(result);")

if [ -z "$CODE_VERIFIER" ]; then
  echo "❌ Не удалось сгенерировать code_verifier. Убедитесь, что Node.js установлен."
  exit 1
fi

echo "✅ Code verifier сгенерирован: ${CODE_VERIFIER:0:20}..."
echo ""

# Generate code_challenge
echo "2. Генерирую code_challenge..."
CODE_CHALLENGE=$(node -e "const crypto = require('crypto'); const hash = crypto.createHash('sha256').update('$CODE_VERIFIER').digest('base64'); console.log(hash.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, ''));")

if [ -z "$CODE_CHALLENGE" ]; then
  echo "❌ Не удалось сгенерировать code_challenge."
  exit 1
fi

echo "✅ Code challenge сгенерирован: ${CODE_CHALLENGE:0:20}..."
echo ""

# Configuration
CLIENT_ID="${OPENL_OAUTH2_CLIENT_ID:-OpenL_Studio}"
AUTHORIZATION_URL="${OPENL_OAUTH2_AUTHORIZATION_URL:-https://testping-sso.eisgroup.com/as/authorization.oauth2}"
# Redirect URI должен быть зарегистрирован в Ping Identity для клиента OpenL_Studio
# Можно переопределить через переменную окружения: export OPENL_OAUTH2_REDIRECT_URI="ваш-uri"
REDIRECT_URI="${OPENL_OAUTH2_REDIRECT_URI:-http://localhost:3000/oauth2/callback}"
SCOPE="${OPENL_OAUTH2_SCOPE:-openid profile email}"

echo "Используемые параметры:"
echo "  Client ID: $CLIENT_ID"
echo "  Redirect URI: $REDIRECT_URI"
echo "  ⚠️  Убедитесь, что этот Redirect URI зарегистрирован в Ping Identity!"
echo ""

# Build authorization URL with PKCE
AUTH_URL="${AUTHORIZATION_URL}?response_type=code&client_id=${CLIENT_ID}&scope=$(echo -n "$SCOPE" | jq -sRr @uri 2>/dev/null || echo "$SCOPE" | sed 's/ /%20/g')&redirect_uri=$(echo -n "$REDIRECT_URI" | jq -sRr @uri 2>/dev/null || echo "$REDIRECT_URI" | sed 's/:/%3A/g; s/\//%2F/g')&code_challenge=${CODE_CHALLENGE}&code_challenge_method=S256"

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

echo "3. Открываю браузер с URL авторизации..."
echo ""

# Open browser automatically
if open_browser "$AUTH_URL"; then
  echo "✅ Браузер открыт с URL авторизации"
else
  echo "URL для ручного открытия:"
  echo "$AUTH_URL"
fi

echo ""
echo "4. Войдите с вашими учетными данными"
echo ""
echo "5. После входа вы будете перенаправлены с параметром 'code' в URL"
echo "   Скопируйте ПОЛНЫЙ URL из адресной строки браузера"
echo ""
read -p "Вставьте полный URL с параметром code: " FULL_URL

# Extract authorization code from URL (macOS compatible - doesn't use -P flag)
# Try to extract code parameter
AUTH_CODE=$(echo "$FULL_URL" | sed -n 's/.*[?&]code=\([^&]*\).*/\1/p' | head -1)
if [ -z "$AUTH_CODE" ]; then
  # Fallback: try to extract from URL directly
  AUTH_CODE=$(echo "$FULL_URL" | sed 's/.*code=\([^&]*\).*/\1/')
fi

# Extract redirect_uri from URL (if present)
EXTRACTED_REDIRECT_URI=$(echo "$FULL_URL" | sed -n 's/.*[?&]redirect_uri=\([^&]*\).*/\1/p' | head -1)
if [ -z "$EXTRACTED_REDIRECT_URI" ]; then
  # If redirect_uri not in URL, use the one from configuration
  EXTRACTED_REDIRECT_URI="$REDIRECT_URI"
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
echo "=========================================="
echo "Обмен authorization code на токены (PKCE)"
echo "=========================================="
echo ""
echo "Используемые параметры:"
echo "  Authorization code: ${AUTH_CODE:0:20}..."
echo "  Code verifier: ${CODE_VERIFIER:0:20}..."
echo "  Redirect URI: $EXTRACTED_REDIRECT_URI"
echo ""

# Get token URL
TOKEN_URL="${OPENL_OAUTH2_TOKEN_URL:-https://testping-sso.eisgroup.com/as/token.oauth2}"

# Exchange authorization code for tokens using PKCE
echo "Отправляю запрос на получение токенов..."
RESPONSE=$(curl -s -X POST "$TOKEN_URL" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code&code=$AUTH_CODE&redirect_uri=$EXTRACTED_REDIRECT_URI&client_id=$CLIENT_ID&code_verifier=$CODE_VERIFIER")

echo ""
echo "Ответ сервера:"
echo "$RESPONSE" | jq . 2>/dev/null || echo "$RESPONSE"

ACCESS_TOKEN=$(echo "$RESPONSE" | jq -r '.access_token' 2>/dev/null)
REFRESH_TOKEN=$(echo "$RESPONSE" | jq -r '.refresh_token' 2>/dev/null)

if [ "$ACCESS_TOKEN" != "null" ] && [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "" ]; then
  echo ""
  echo "=========================================="
  echo "✅ Токены успешно получены!"
  echo "=========================================="
  echo ""
  echo "Для использования PKCE в Docker (compose.yaml), добавьте:"
  echo ""
  echo "  OPENL_OAUTH2_GRANT_TYPE: authorization_code"
  echo "  OPENL_OAUTH2_AUTHORIZATION_CODE: \"$AUTH_CODE\""
  echo "  OPENL_OAUTH2_CODE_VERIFIER: \"$CODE_VERIFIER\""
  echo "  OPENL_OAUTH2_REDIRECT_URI: \"$EXTRACTED_REDIRECT_URI\""
  echo ""
  if [ "$REFRESH_TOKEN" != "null" ] && [ -n "$REFRESH_TOKEN" ] && [ "$REFRESH_TOKEN" != "" ]; then
    echo "Или используйте refresh_token (рекомендуется для долгосрочного использования):"
    echo ""
    echo "  OPENL_OAUTH2_GRANT_TYPE: refresh_token"
    echo "  OPENL_OAUTH2_REFRESH_TOKEN: \"$REFRESH_TOKEN\""
    echo ""
  fi
  echo "⚠️  ВАЖНО: Сохраните code_verifier - он нужен для обмена authorization_code!"
  echo "   Code verifier: $CODE_VERIFIER"
  echo ""
else
  echo ""
  echo "=========================================="
  echo "❌ Не удалось получить токены"
  echo "=========================================="
  echo ""
  echo "Проверьте:"
  echo "1. Authorization code правильный и не истек (коды обычно действительны 1-10 минут)"
  echo "2. Code verifier совпадает с тем, что использовался в authorization URL"
  echo "3. Redirect URI совпадает: $EXTRACTED_REDIRECT_URI"
  echo "4. Клиент настроен для authorization_code grant type с PKCE"
  echo ""
  echo "Code verifier, который использовался: $CODE_VERIFIER"
  exit 1
fi

