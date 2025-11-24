# Получение конфигурации Ping Identity для MCP Server

## Обзор

Это руководство поможет вам получить все необходимые параметры OAuth 2.1 из Ping Identity для настройки MCP сервера.

## Необходимые параметры

Для работы с Ping Identity вам понадобятся:

1. **Client ID** (`OPENL_OAUTH2_CLIENT_ID`)
2. **Client Secret** (`OPENL_OAUTH2_CLIENT_SECRET`)
3. **Issuer URI** (`OPENL_OAUTH2_ISSUER_URI`) или **Token URL** (`OPENL_OAUTH2_TOKEN_URL`)
4. **Authorization URL** (`OPENL_OAUTH2_AUTHORIZATION_URL`) - для authorization_code flow
5. **Redirect URI** (`OPENL_OAUTH2_REDIRECT_URI`) - для authorization_code flow
6. **Scopes** (`OPENL_OAUTH2_SCOPE`) - опционально

## Способ 1: Через Ping Identity Admin Console (Рекомендуется)

### Шаг 1: Войдите в Ping Identity Admin Console

1. Откройте админ-панель Ping Identity (обычно `https://your-ping-instance.com/console`)
2. Войдите с учетными данными администратора

### Шаг 2: Найдите OAuth Client

1. Перейдите в раздел **Applications** → **OAuth Clients** (или **Applications** → **Clients**)
2. Найдите клиент `OpenL_Studio` (или создайте новый)
3. Откройте настройки клиента

### Шаг 3: Получите параметры конфигурации

#### Client ID и Client Secret

В настройках клиента вы найдете:

- **Client ID**: Обычно отображается в верхней части страницы
  - Пример: `OpenL_Studio`
  
- **Client Secret**: 
  - Может быть скрыт (показан как `****`)
  - Нажмите "Show" или "Reveal" чтобы увидеть
  - Если секрет не установлен, создайте новый
  - Пример: `Exigen/2024.02`

#### Endpoints (URLs)

В разделе **Endpoints** или **OAuth Settings** найдите:

- **Issuer URI**: Базовый URL Ping Identity сервера
  - Пример: `https://testping-sso.eisgroup.com`
  
- **Token Endpoint**: URL для получения токенов
  - Ping Identity обычно использует: `{issuer-uri}/as/token.oauth2`
  - Пример: `https://testping-sso.eisgroup.com/as/token.oauth2`
  
- **Authorization Endpoint**: URL для авторизации пользователя
  - Ping Identity обычно использует: `{issuer-uri}/as/authorization.oauth2`
  - Пример: `https://testping-sso.eisgroup.com/as/authorization.oauth2`

#### Redirect URIs

В разделе **Redirect URIs** или **Allowed Redirect URIs**:

- Убедитесь, что ваш redirect URI зарегистрирован
- Примеры:
  - `https://dc3eisovpn03-fast.eisgroup.com/oauth2/callback`
  - `http://localhost:3000/oauth2/callback`
  - `http://localhost:8080/login/oauth2/code/webstudio`

#### Grant Types

В разделе **Grant Types** или **Allowed Grant Types**:

- Убедитесь, что включены нужные grant types:
  - ✅ `authorization_code` (для PKCE и refresh_token flow)
  - ✅ `refresh_token` (для обновления токенов)
  - ✅ `client_credentials` (для service-to-service)

#### Scopes

В разделе **Scopes** или **Allowed Scopes**:

- Убедитесь, что нужные scopes разрешены:
  - `openid`
  - `profile`
  - `email`
  - Или кастомные scopes для вашего приложения

## Способ 2: Через .well-known/openid-configuration

Если у вас есть Issuer URI, вы можете автоматически получить конфигурацию:

```bash
# Замените на ваш Issuer URI
ISSUER_URI="https://testping-sso.eisgroup.com"

# Получить конфигурацию
curl "$ISSUER_URI/.well-known/openid-configuration" | jq .
```

Ответ будет содержать:
```json
{
  "issuer": "https://testping-sso.eisgroup.com",
  "authorization_endpoint": "https://testping-sso.eisgroup.com/as/authorization.oauth2",
  "token_endpoint": "https://testping-sso.eisgroup.com/as/token.oauth2",
  "userinfo_endpoint": "https://testping-sso.eisgroup.com/as/userinfo.oauth2",
  "jwks_uri": "https://testping-sso.eisgroup.com/as/jwks.oauth2",
  "scopes_supported": ["openid", "profile", "email"],
  "response_types_supported": ["code", "token", "id_token"],
  ...
}
```

**Важно:** Ping Identity может использовать нестандартные пути для endpoints. Если `.well-known` не содержит правильные endpoints, используйте значения из админ-панели.

## Способ 3: Через существующую конфигурацию

Если у вас уже есть рабочая конфигурация (например, в OpenL Studio), вы можете использовать те же параметры:

1. Найдите конфигурацию OAuth в OpenL Studio
2. Скопируйте параметры:
   - Client ID
   - Client Secret
   - Token URL
   - Authorization URL
   - Redirect URI

## Конфигурация для разных Grant Types

### Client Credentials (Service-to-Service)

```yaml
environment:
  OPENL_OAUTH2_CLIENT_ID: OpenL_Studio
  OPENL_OAUTH2_CLIENT_SECRET: Exigen/2024.02
  OPENL_OAUTH2_TOKEN_URL: https://testping-sso.eisgroup.com/as/token.oauth2
  OPENL_OAUTH2_GRANT_TYPE: client_credentials
  OPENL_OAUTH2_USE_BASIC_AUTH: "true"  # Ping Identity требует Basic Auth
  OPENL_OAUTH2_SCOPE: "openid profile email"  # Опционально
```

### Authorization Code с PKCE

```yaml
environment:
  OPENL_OAUTH2_CLIENT_ID: OpenL_Studio
  # OPENL_OAUTH2_CLIENT_SECRET: не требуется для PKCE (публичный клиент)
  OPENL_OAUTH2_TOKEN_URL: https://testping-sso.eisgroup.com/as/token.oauth2
  OPENL_OAUTH2_AUTHORIZATION_URL: https://testping-sso.eisgroup.com/as/authorization.oauth2
  OPENL_OAUTH2_GRANT_TYPE: authorization_code
  OPENL_OAUTH2_AUTHORIZATION_CODE: "полученный_authorization_code"
  OPENL_OAUTH2_CODE_VERIFIER: "сгенерированный_code_verifier"
  OPENL_OAUTH2_REDIRECT_URI: "https://dc3eisovpn03-fast.eisgroup.com/oauth2/callback"
  OPENL_OAUTH2_SCOPE: "openid profile email"
```

### Refresh Token

```yaml
environment:
  OPENL_OAUTH2_CLIENT_ID: OpenL_Studio
  OPENL_OAUTH2_CLIENT_SECRET: Exigen/2024.02
  OPENL_OAUTH2_TOKEN_URL: https://testping-sso.eisgroup.com/as/token.oauth2
  OPENL_OAUTH2_GRANT_TYPE: refresh_token
  OPENL_OAUTH2_REFRESH_TOKEN: "ваш_refresh_token"
  # OPENL_OAUTH2_USE_BASIC_AUTH: может потребоваться в зависимости от настроек Ping Identity
```

## Особенности Ping Identity

### 1. Token Endpoint Path

Ping Identity использует нестандартный путь для token endpoint:
- **Стандартный OAuth2**: `/token`
- **Ping Identity**: `/as/token.oauth2`

### 2. Basic Authentication

Ping Identity часто требует Basic Authentication header вместо отправки credentials в body:

```bash
OPENL_OAUTH2_USE_BASIC_AUTH=true
```

Это отправляет `Authorization: Basic base64(client_id:client_secret)` header.

### 3. Authorization Endpoint Path

Ping Identity использует:
- **Стандартный OAuth2**: `/oauth/authorize`
- **Ping Identity**: `/as/authorization.oauth2`

## Проверка конфигурации

После получения параметров, проверьте их:

### 1. Проверка Token Endpoint

```bash
curl -X POST https://testping-sso.eisgroup.com/as/token.oauth2 \
  -H "Authorization: Basic $(echo -n 'OpenL_Studio:Exigen/2024.02' | base64)" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials"
```

Если получите токен - конфигурация правильная!

### 2. Проверка Authorization Endpoint

Откройте в браузере:
```
https://testping-sso.eisgroup.com/as/authorization.oauth2?response_type=code&client_id=OpenL_Studio&scope=openid%20profile%20email&redirect_uri=http://localhost:8080/oauth2/callback
```

Если увидите страницу входа - endpoint правильный!

## Получение Refresh Token

После получения всех параметров, используйте скрипт для получения refresh_token:

```bash
cd mcp-server
./get-refresh-token.sh
```

Скрипт использует:
- Authorization URL для получения authorization_code
- Token URL для обмена на refresh_token
- Redirect URI из конфигурации

## Пример полной конфигурации для compose.yaml

```yaml
mcp-server:
  environment:
    PORT: 3000
    OPENL_BASE_URL: http://studio:8080/rest
    
    # Ping Identity OAuth 2.1 Configuration
    OPENL_OAUTH2_CLIENT_ID: OpenL_Studio
    OPENL_OAUTH2_CLIENT_SECRET: Exigen/2024.02
    OPENL_OAUTH2_ISSUER_URI: https://testping-sso.eisgroup.com
    # Или явно укажите token URL:
    # OPENL_OAUTH2_TOKEN_URL: https://testping-sso.eisgroup.com/as/token.oauth2
    
    # Для refresh_token flow:
    OPENL_OAUTH2_GRANT_TYPE: refresh_token
    OPENL_OAUTH2_REFRESH_TOKEN: "ваш_refresh_token_здесь"
    
    # Для client_credentials flow:
    # OPENL_OAUTH2_GRANT_TYPE: client_credentials
    # OPENL_OAUTH2_USE_BASIC_AUTH: "true"
    
    OPENL_CLIENT_DOCUMENT_ID: docker-compose-1
    NODE_ENV: production
```

## Troubleshooting

### Проблема: "invalid_client"

**Решение:**
- Проверьте Client ID и Client Secret в админ-панели Ping Identity
- Убедитесь, что клиент активен и не заблокирован

### Проблема: "404 Not Found" на token endpoint

**Решение:**
- Используйте явный `OPENL_OAUTH2_TOKEN_URL` вместо `OPENL_OAUTH2_ISSUER_URI`
- Убедитесь, что путь `/as/token.oauth2` правильный для вашего Ping Identity

### Проблема: "unauthorized_client"

**Решение:**
- Проверьте, что grant type разрешен для клиента в Ping Identity
- Убедитесь, что redirect URI зарегистрирован
- Проверьте scopes - они должны быть разрешены для клиента

### Проблема: "invalid_grant" при использовании refresh_token

**Решение:**
- Refresh token истек - получите новый через authorization_code flow
- Проверьте, что refresh_token не был отозван в Ping Identity

## Дополнительные ресурсы

- [Ping Identity OAuth 2.0 Documentation](https://docs.pingidentity.com/)
- [MCP Server Authentication Guide](./AUTHENTICATION.md)
- [PKCE Setup Guide](./PKCE-SETUP.md)
- [Refresh Token Guide](./REFRESH-TOKEN-EXPLAINED.md)

## Быстрая справка

| Параметр | Где найти | Пример |
|----------|-----------|--------|
| Client ID | Ping Identity Admin → Applications → Client → Client ID | `OpenL_Studio` |
| Client Secret | Ping Identity Admin → Applications → Client → Client Secret | `Exigen/2024.02` |
| Issuer URI | Ping Identity Admin → Environment → Base URL | `https://testping-sso.eisgroup.com` |
| Token URL | Issuer URI + `/as/token.oauth2` | `https://testping-sso.eisgroup.com/as/token.oauth2` |
| Auth URL | Issuer URI + `/as/authorization.oauth2` | `https://testping-sso.eisgroup.com/as/authorization.oauth2` |
| Redirect URI | Зарегистрирован в клиенте | `https://dc3eisovpn03-fast.eisgroup.com/oauth2/callback` |

