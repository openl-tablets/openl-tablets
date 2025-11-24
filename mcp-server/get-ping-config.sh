#!/bin/bash

echo "=========================================="
echo "Получение конфигурации Ping Identity"
echo "=========================================="
echo ""

# Default values
ISSUER_URI="${OPENL_OAUTH2_ISSUER_URI:-https://testping-sso.eisgroup.com}"
CLIENT_ID="${OPENL_OAUTH2_CLIENT_ID:-OpenL_Studio}"

echo "Используемые параметры:"
echo "  Issuer URI: $ISSUER_URI"
echo "  Client ID: $CLIENT_ID"
echo ""

# Get configuration from .well-known endpoint
echo "1. Получаю конфигурацию из .well-known/openid-configuration..."
echo ""

WELL_KNOWN_URL="$ISSUER_URI/.well-known/openid-configuration"
CONFIG=$(curl -s "$WELL_KNOWN_URL")

if [ -z "$CONFIG" ] || echo "$CONFIG" | grep -q "error"; then
  echo "❌ Не удалось получить конфигурацию из $WELL_KNOWN_URL"
  echo ""
  echo "Проверьте:"
  echo "1. Issuer URI правильный: $ISSUER_URI"
  echo "2. Ping Identity сервер доступен"
  echo "3. .well-known endpoint не заблокирован"
  exit 1
fi

echo "✅ Конфигурация получена!"
echo ""

# Extract endpoints using jq if available, otherwise use sed
if command -v jq &> /dev/null; then
  TOKEN_ENDPOINT=$(echo "$CONFIG" | jq -r '.token_endpoint // empty')
  AUTH_ENDPOINT=$(echo "$CONFIG" | jq -r '.authorization_endpoint // empty')
  ISSUER=$(echo "$CONFIG" | jq -r '.issuer // empty')
  SCOPES=$(echo "$CONFIG" | jq -r '.scopes_supported[]? // empty' | tr '\n' ' ')
else
  # Fallback to sed if jq not available
  TOKEN_ENDPOINT=$(echo "$CONFIG" | sed -n 's/.*"token_endpoint":\s*"\([^"]*\)".*/\1/p')
  AUTH_ENDPOINT=$(echo "$CONFIG" | sed -n 's/.*"authorization_endpoint":\s*"\([^"]*\)".*/\1/p')
  ISSUER=$(echo "$CONFIG" | sed -n 's/.*"issuer":\s*"\([^"]*\)".*/\1/p')
fi

echo "=========================================="
echo "Конфигурация Ping Identity"
echo "=========================================="
echo ""
echo "📋 Endpoints:"
echo "  Issuer URI: ${ISSUER:-$ISSUER_URI}"
echo "  Token Endpoint: ${TOKEN_ENDPOINT:-$ISSUER_URI/as/token.oauth2}"
echo "  Authorization Endpoint: ${AUTH_ENDPOINT:-$ISSUER_URI/as/authorization.oauth2}"
echo ""

if [ -n "$SCOPES" ]; then
  echo "📋 Поддерживаемые Scopes:"
  echo "  $SCOPES"
  echo ""
fi

echo "=========================================="
echo "Конфигурация для compose.yaml"
echo "=========================================="
echo ""
echo "Добавьте следующие параметры в compose.yaml (секция mcp-server environment):"
echo ""
echo "  # Ping Identity OAuth 2.1 Configuration"
echo "  OPENL_OAUTH2_CLIENT_ID: $CLIENT_ID"
echo "  OPENL_OAUTH2_CLIENT_SECRET: \"ВАШ_CLIENT_SECRET_ЗДЕСЬ\""
echo "  OPENL_OAUTH2_ISSUER_URI: ${ISSUER:-$ISSUER_URI}"
echo "  # Или явно укажите token URL:"
echo "  # OPENL_OAUTH2_TOKEN_URL: ${TOKEN_ENDPOINT:-$ISSUER_URI/as/token.oauth2}"
echo ""

echo "Для refresh_token flow:"
echo "  OPENL_OAUTH2_GRANT_TYPE: refresh_token"
echo "  OPENL_OAUTH2_REFRESH_TOKEN: \"ВАШ_REFRESH_TOKEN_ЗДЕСЬ\""
echo ""

echo "Для client_credentials flow:"
echo "  OPENL_OAUTH2_GRANT_TYPE: client_credentials"
echo "  OPENL_OAUTH2_USE_BASIC_AUTH: \"true\"  # Ping Identity требует Basic Auth"
echo ""

echo "Для authorization_code с PKCE:"
echo "  OPENL_OAUTH2_GRANT_TYPE: authorization_code"
echo "  OPENL_OAUTH2_AUTHORIZATION_URL: ${AUTH_ENDPOINT:-$ISSUER_URI/as/authorization.oauth2}"
echo "  OPENL_OAUTH2_AUTHORIZATION_CODE: \"ВАШ_AUTHORIZATION_CODE\""
echo "  OPENL_OAUTH2_CODE_VERIFIER: \"ВАШ_CODE_VERIFIER\""
echo "  OPENL_OAUTH2_REDIRECT_URI: \"ВАШ_REDIRECT_URI\""
echo ""

echo "=========================================="
echo "Полная конфигурация (пример)"
echo "=========================================="
echo ""
cat << EOF
mcp-server:
  environment:
    PORT: 3000
    OPENL_BASE_URL: http://studio:8080/rest
    
    # Ping Identity OAuth 2.1 Configuration
    OPENL_OAUTH2_CLIENT_ID: $CLIENT_ID
    OPENL_OAUTH2_CLIENT_SECRET: "ВАШ_CLIENT_SECRET"
    OPENL_OAUTH2_ISSUER_URI: ${ISSUER:-$ISSUER_URI}
    
    # Для refresh_token flow:
    OPENL_OAUTH2_GRANT_TYPE: refresh_token
    OPENL_OAUTH2_REFRESH_TOKEN: "ВАШ_REFRESH_TOKEN"
    
    OPENL_CLIENT_DOCUMENT_ID: docker-compose-1
    NODE_ENV: production
EOF
echo ""

echo "=========================================="
echo "Следующие шаги"
echo "=========================================="
echo ""
echo "1. Получите Client Secret из Ping Identity Admin Console"
echo "2. Получите Refresh Token используя: ./get-refresh-token.sh"
echo "3. Обновите compose.yaml с полученными значениями"
echo "4. Перезапустите Docker: docker compose restart mcp-server"
echo ""

