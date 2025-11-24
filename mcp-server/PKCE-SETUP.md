# Настройка OAuth 2.1 PKCE для MCP Server в Docker

## Текущая ситуация

Сейчас у вас настроен **refresh_token flow**, который уже работает. Если вы хотите использовать **PKCE flow**, выполните следующие шаги.

## Когда использовать PKCE?

- ✅ **Публичные клиенты** без `client_secret`
- ✅ **Повышенная безопасность** для authorization code flow
- ✅ **Соответствие OAuth 2.1** (PKCE рекомендуется для всех authorization code flows)

## Быстрый старт с PKCE

### Вариант 1: Использовать готовый скрипт (рекомендуется)

```bash
cd mcp-server
./get-pkce-token.sh
```

Скрипт автоматически:
1. Сгенерирует `code_verifier` и `code_challenge`
2. Покажет URL для авторизации в браузере
3. Поможет получить `authorization_code`
4. Обменяет код на токены
5. Покажет конфигурацию для `compose.yaml`

### Вариант 2: Ручная настройка

#### Шаг 1: Генерация code_verifier

```bash
node -e "const crypto = require('crypto'); const charset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~'; const random = crypto.randomBytes(128); let result = ''; for (let i = 0; i < 128; i++) { result += charset[random[i] % charset.length]; } console.log(result);"
```

Сохраните полученный `code_verifier` - он понадобится позже.

#### Шаг 2: Генерация code_challenge

```bash
CODE_VERIFIER="ваш_code_verifier_здесь"
node -e "const crypto = require('crypto'); const hash = crypto.createHash('sha256').update('$CODE_VERIFIER').digest('base64'); console.log(hash.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, ''));"
```

#### Шаг 3: Получение authorization_code

Откройте в браузере URL авторизации с PKCE параметрами:

```
https://testping-sso.eisgroup.com/as/authorization.oauth2?response_type=code&client_id=OpenL_Studio&scope=openid%20profile%20email&redirect_uri=https://dc3eisovpn03-fast.eisgroup.com/oauth2/callback&code_challenge=ВАШ_CODE_CHALLENGE&code_challenge_method=S256
```

После авторизации вы будете перенаправлены на `redirect_uri` с параметром `code` в URL. Скопируйте этот `authorization_code`.

#### Шаг 4: Обмен authorization_code на токены

```bash
curl -X POST https://testping-sso.eisgroup.com/as/token.oauth2 \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code&code=ВАШ_AUTHORIZATION_CODE&redirect_uri=https://dc3eisovpn03-fast.eisgroup.com/oauth2/callback&client_id=OpenL_Studio&code_verifier=ВАШ_CODE_VERIFIER"
```

## Конфигурация для Docker (compose.yaml)

После получения `authorization_code` и `code_verifier`, обновите `compose.yaml`:

```yaml
mcp-server:
  environment:
    PORT: 3000
    OPENL_BASE_URL: http://studio:8080/rest
    
    # PKCE Configuration
    OPENL_OAUTH2_CLIENT_ID: OpenL_Studio
    # OPENL_OAUTH2_CLIENT_SECRET: не требуется для PKCE!
    OPENL_OAUTH2_TOKEN_URL: https://testping-sso.eisgroup.com/as/token.oauth2
    OPENL_OAUTH2_AUTHORIZATION_URL: https://testping-sso.eisgroup.com/as/authorization.oauth2
    OPENL_OAUTH2_GRANT_TYPE: authorization_code
    OPENL_OAUTH2_AUTHORIZATION_CODE: "ваш_authorization_code"
    OPENL_OAUTH2_CODE_VERIFIER: "ваш_code_verifier"
    OPENL_OAUTH2_REDIRECT_URI: "https://dc3eisovpn03-fast.eisgroup.com/oauth2/callback"
    OPENL_OAUTH2_SCOPE: "openid profile email"
    
    OPENL_CLIENT_DOCUMENT_ID: docker-compose-1
    NODE_ENV: production
```

## Важные замечания

1. **Authorization code действителен ограниченное время** (обычно 1-10 минут)
   - Используйте его сразу после получения
   - После обмена на токены, используйте `refresh_token` для долгосрочной работы

2. **Code verifier должен совпадать**
   - Тот же `code_verifier`, который использовался для генерации `code_challenge` в authorization URL
   - Должен использоваться при обмене authorization_code на токены

3. **Redirect URI должен совпадать**
   - Должен быть зарегистрирован в OAuth провайдере
   - Должен совпадать в authorization URL и token exchange

4. **После получения refresh_token**
   - Рекомендуется переключиться на `refresh_token` grant type
   - `refresh_token` долгоживущий и не требует повторной авторизации

## Переключение на refresh_token (после первого успешного PKCE)

После успешного обмена authorization_code на токены, вы получите `refresh_token`. Используйте его для долгосрочной работы:

```yaml
mcp-server:
  environment:
    # ... другие параметры ...
    OPENL_OAUTH2_GRANT_TYPE: refresh_token
    OPENL_OAUTH2_REFRESH_TOKEN: "ваш_refresh_token"
    # Можно удалить authorization_code и code_verifier после получения refresh_token
```

## Сравнение методов аутентификации

| Метод | Требует client_secret | Требует user interaction | Безопасность | Рекомендуется для |
|-------|----------------------|-------------------------|--------------|-------------------|
| **client_credentials** | ✅ Да | ❌ Нет | ⭐⭐⭐ | Service-to-service |
| **authorization_code + PKCE** | ❌ Нет | ✅ Да | ⭐⭐⭐⭐⭐ | Публичные клиенты |
| **refresh_token** | ✅ Да* | ❌ Нет | ⭐⭐⭐⭐ | Долгосрочное использование |

*Для refresh_token обычно требуется client_secret, но некоторые провайдеры поддерживают refresh_token без client_secret

## Troubleshooting

### Ошибка: "invalid_grant"
- Проверьте, что `authorization_code` не истек (коды действительны 1-10 минут)
- Убедитесь, что `code_verifier` совпадает с тем, что использовался в authorization URL
- Проверьте, что `redirect_uri` совпадает

### Ошибка: "invalid_client"
- Убедитесь, что клиент настроен для `authorization_code` grant type
- Проверьте, что клиент поддерживает PKCE (code_challenge_method=S256)

### Ошибка: "unauthorized_client"
- Проверьте, что `redirect_uri` зарегистрирован в OAuth провайдере
- Убедитесь, что клиент имеет права на использование authorization_code flow

## Дополнительная информация

- [AUTHENTICATION.md](./AUTHENTICATION.md) - Полная документация по аутентификации
- [CURSOR-DOCKER-SETUP.md](./CURSOR-DOCKER-SETUP.md) - Настройка подключения Cursor к Docker
- [RFC 7636 - PKCE](https://tools.ietf.org/html/rfc7636) - Спецификация PKCE

