# Получение Refresh Token для Ping Identity

## Шаг 1: Авторизация в браузере

1. Откройте в браузере URL авторизации:
   ```
   https://testping-sso.eisgroup.com/as/authorization.oauth2?response_type=code&client_id=OpenL_Studio&scope=openid%20profile%20email&redirect_uri=http://localhost:8080/login/oauth2/code/webstudio
   ```

2. Войдите с вашими учетными данными

3. После успешного входа вы будете перенаправлены на `redirect_uri` с параметром `code` в URL:
   ```
   http://localhost:8080/login/oauth2/code/webstudio?code=AUTHORIZATION_CODE&state=...
   ```

## Шаг 2: Обмен authorization code на refresh token

Скопируйте `AUTHORIZATION_CODE` из URL и выполните:

```bash
curl -X POST https://testping-sso.eisgroup.com/as/token.oauth2 \
  -H "Authorization: Basic $(echo -n 'OpenL_Studio:Exigen/2024.02' | base64)" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code&code=AUTHORIZATION_CODE&redirect_uri=http://localhost:8080/login/oauth2/code/webstudio"
```

Или используйте скрипт ниже.

## Шаг 3: Сохраните refresh_token

В ответе вы получите JSON с `access_token` и `refresh_token`. Сохраните `refresh_token` - он будет использоваться для автоматического получения новых access tokens.

## Автоматический скрипт

Создайте файл `get-refresh-token.sh`:

```bash
#!/bin/bash

echo "1. Откройте в браузере:"
echo "https://testping-sso.eisgroup.com/as/authorization.oauth2?response_type=code&client_id=OpenL_Studio&scope=openid%20profile%20email&redirect_uri=http://localhost:8080/login/oauth2/code/webstudio"
echo ""
echo "2. После входа скопируйте authorization code из URL (параметр 'code')"
echo ""
read -p "Введите authorization code: " AUTH_CODE

echo ""
echo "Обмениваю код на токены..."

RESPONSE=$(curl -s -X POST https://testping-sso.eisgroup.com/as/token.oauth2 \
  -H "Authorization: Basic $(echo -n 'OpenL_Studio:Exigen/2024.02' | base64)" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code&code=$AUTH_CODE&redirect_uri=http://localhost:8080/login/oauth2/code/webstudio")

echo "$RESPONSE" | jq .

REFRESH_TOKEN=$(echo "$RESPONSE" | jq -r '.refresh_token')

if [ "$REFRESH_TOKEN" != "null" ] && [ -n "$REFRESH_TOKEN" ]; then
  echo ""
  echo "✅ Refresh Token получен!"
  echo ""
  echo "Добавьте в compose.yaml:"
  echo "  OPENL_OAUTH2_REFRESH_TOKEN: \"$REFRESH_TOKEN\""
  echo "  OPENL_OAUTH2_GRANT_TYPE: refresh_token"
else
  echo ""
  echo "❌ Не удалось получить refresh_token. Проверьте ответ выше."
fi
```

