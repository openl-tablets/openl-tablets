# Personal Access Token (PAT) API Reference

**Version**: 6.0.0-SNAPSHOT
**Module**: STUDIO
**Feature**: EPBDS-15458
**Last Updated**: 2025-12-23

---

## Table of Contents

- [Overview](#overview)
- [Authentication](#authentication)
- [API Endpoints](#api-endpoints)
  - [Create Personal Access Token](#create-personal-access-token)
  - [List Personal Access Tokens](#list-personal-access-tokens)
  - [Get Personal Access Token](#get-personal-access-token)
  - [Delete Personal Access Token](#delete-personal-access-token)
- [Data Models](#data-models)
- [Error Handling](#error-handling)
- [Usage Examples](#usage-examples)
- [Security Considerations](#security-considerations)

---

## Overview

The Personal Access Token (PAT) API enables users to generate and manage authentication tokens for programmatic access to OpenL Tablets Studio. PATs provide an alternative to OAuth2/SAML authentication for service-to-service communication and API integrations.

### Key Features

- **Secure Token Generation**: Cryptographically secure tokens using Base62 encoding
- **Token Management**: Full CRUD operations for personal access tokens
- **Expiration Support**: Optional token expiration for enhanced security
- **User Isolation**: Users can only manage their own tokens
- **OAuth2/SAML Only**: Available only in OAuth2 and SAML authentication modes

### Token Format

Personal Access Tokens follow this format:

```
openl_pat_<publicId>.<secret>
```

Where:
- `publicId`: 16-character Base62-encoded public identifier
- `secret`: 32-character Base62-encoded secret

**Example**:
```
openl_pat_a1B2c3D4e5F6g7H8.i9J0k1L2m3N4o5P6q7R8s9T0u1V2w3X4y5Z6
```

---

## Authentication

### Prerequisites

PAT management endpoints require:
1. **User Mode**: `oauth2` or `saml` (configured in `application.properties`)
2. **Authentication**: Valid OAuth2/SAML Bearer token
3. **Restriction**: PAT authentication **cannot** be used to manage PATs (enforced by `@NotPatAuth`)

### Using PATs for API Access

Once created, PATs can be used to authenticate API requests:

```http
GET /rest/api/endpoint HTTP/1.1
Authorization: Token openl_pat_a1B2c3D4e5F6g7H8.i9J0k1L2m3N4o5P6q7R8s9T0u1V2w3X4y5Z6
```

**Important**: Use `Token` prefix, not `Bearer`.

---

## API Endpoints

### Base URL

```
/rest/users/personal-access-tokens
```

All endpoints return JSON and require `Content-Type: application/json`.

---

### Create Personal Access Token

Creates a new Personal Access Token for the authenticated user.

#### Endpoint

```http
POST /rest/users/personal-access-tokens
```

#### Request Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <oauth2-token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Request Body

```json
{
  "name": "MCP Client Token",
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | String | Yes | Max 100 chars, not blank | Human-readable token name (must be unique per user) |
| `expiresAt` | ISO 8601 DateTime | No | Must be future date | Token expiration date (null = never expires) |

#### Response

**Status**: `201 Created`

```json
{
  "publicId": "a1B2c3D4e5F6g7H8",
  "name": "MCP Client Token",
  "loginName": "john.doe",
  "token": "openl_pat_a1B2c3D4e5F6g7H8.i9J0k1L2m3N4o5P6q7R8s9T0u1V2w3X4y5Z6",
  "createdAt": "2025-12-23T10:30:00Z",
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

**Important**: The `token` field contains the full token value and is **shown only once**. Store it securely - it cannot be retrieved later.

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| `400 Bad Request` | `pat.duplicate.name.message` | Token with this name already exists |
| `400 Bad Request` | - | Token name is blank or too long |
| `400 Bad Request` | - | Expiration date is in the past |
| `401 Unauthorized` | - | Invalid or missing Bearer token |
| `403 Forbidden` | - | User mode is not OAuth2/SAML |
| `403 Forbidden` | - | Request authenticated with PAT (not allowed) |

---

### List Personal Access Tokens

Retrieves all Personal Access Tokens for the authenticated user.

#### Endpoint

```http
GET /rest/users/personal-access-tokens
```

#### Request Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <oauth2-token>` | Yes |

#### Response

**Status**: `200 OK`

```json
[
  {
    "publicId": "a1B2c3D4e5F6g7H8",
    "name": "MCP Client Token",
    "loginName": "john.doe",
    "createdAt": "2025-12-23T10:30:00Z",
    "expiresAt": "2026-12-31T23:59:59Z"
  },
  {
    "publicId": "z9Y8x7W6v5U4t3S2",
    "name": "CI/CD Pipeline",
    "loginName": "john.doe",
    "createdAt": "2025-11-15T08:00:00Z",
    "expiresAt": null
  }
]
```

**Note**: Token secrets are **never** returned by this endpoint.

#### Error Responses

| Status | Description |
|--------|-------------|
| `401 Unauthorized` | Invalid or missing Bearer token |
| `403 Forbidden` | User mode is not OAuth2/SAML |
| `403 Forbidden` | Request authenticated with PAT (not allowed) |

---

### Get Personal Access Token

Retrieves a specific Personal Access Token by its public ID.

#### Endpoint

```http
GET /rest/users/personal-access-tokens/{publicId}
```

#### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `publicId` | String | The 16-character public identifier of the token |

#### Request Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <oauth2-token>` | Yes |

#### Response

**Status**: `200 OK`

```json
{
  "publicId": "a1B2c3D4e5F6g7H8",
  "name": "MCP Client Token",
  "loginName": "john.doe",
  "createdAt": "2025-12-23T10:30:00Z",
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| `401 Unauthorized` | - | Invalid or missing Bearer token |
| `403 Forbidden` | - | User mode is not OAuth2/SAML |
| `403 Forbidden` | - | Request authenticated with PAT (not allowed) |
| `404 Not Found` | `pat.not.found.message` | Token not found or doesn't belong to user |

---

### Delete Personal Access Token

Deletes a Personal Access Token, immediately revoking access.

#### Endpoint

```http
DELETE /rest/users/personal-access-tokens/{publicId}
```

#### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `publicId` | String | The 16-character public identifier of the token |

#### Request Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <oauth2-token>` | Yes |

#### Response

**Status**: `204 No Content`

No response body.

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| `401 Unauthorized` | - | Invalid or missing Bearer token |
| `403 Forbidden` | - | User mode is not OAuth2/SAML |
| `403 Forbidden` | - | Request authenticated with PAT (not allowed) |
| `404 Not Found` | `pat.not.found.message` | Token not found or doesn't belong to user |

---

## Data Models

### CreatePersonalAccessTokenRequest

Request model for creating a new token.

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | String | Yes | 1-100 characters | Unique name for the token (per user) |
| `expiresAt` | ISO 8601 DateTime | No | Future date or null | Expiration date (null = never expires) |

**Example**:
```json
{
  "name": "API Integration Token",
  "expiresAt": "2026-06-30T23:59:59Z"
}
```

---

### CreatedPersonalAccessTokenResponse

Response model returned when a token is created. **Contains the full token - shown only once!**

| Field | Type | Description |
|-------|------|-------------|
| `publicId` | String | 16-character public identifier |
| `name` | String | Token name |
| `loginName` | String | Owner's login name |
| `token` | String | **Full token value** (only shown once) |
| `createdAt` | ISO 8601 DateTime | Creation timestamp |
| `expiresAt` | ISO 8601 DateTime | Expiration timestamp (null = never expires) |

**Example**:
```json
{
  "publicId": "a1B2c3D4e5F6g7H8",
  "name": "API Integration Token",
  "loginName": "john.doe",
  "token": "openl_pat_a1B2c3D4e5F6g7H8.i9J0k1L2m3N4o5P6q7R8s9T0u1V2w3X4y5Z6",
  "createdAt": "2025-12-23T10:30:00Z",
  "expiresAt": "2026-06-30T23:59:59Z"
}
```

---

### PersonalAccessTokenResponse

Response model for listing and retrieving tokens (without secret).

| Field | Type | Description |
|-------|------|-------------|
| `publicId` | String | 16-character public identifier |
| `name` | String | Token name |
| `loginName` | String | Owner's login name |
| `createdAt` | ISO 8601 DateTime | Creation timestamp |
| `expiresAt` | ISO 8601 DateTime | Expiration timestamp (null = never expires) |

**Example**:
```json
{
  "publicId": "a1B2c3D4e5F6g7H8",
  "name": "API Integration Token",
  "loginName": "john.doe",
  "createdAt": "2025-12-23T10:30:00Z",
  "expiresAt": "2026-06-30T23:59:59Z"
}
```

---

## Error Handling

### Error Response Format

All error responses follow format:

```json
{
  "code": "token.name.required",
  "message": "Token name is required"
}
```

### Common Error Codes

| HTTP Status | Scenario | Message Key |
|-------------|----------|-------------|
| `400 Bad Request` | Duplicate token name | `pat.duplicate.name.message` |
| `400 Bad Request` | Validation error | Varies (Jakarta Validation) |
| `401 Unauthorized` | Missing/invalid auth | - |
| `403 Forbidden` | PAT used to manage PATs | - |
| `403 Forbidden` | Wrong user mode | - |
| `404 Not Found` | Token not found | `pat.not.found.message` |

### Internationalization

Error messages support i18n via `ValidationMessages.properties` and `openapi.properties`:

```properties
# ValidationMessages.properties
pat.duplicate.name.message=A token with this name already exists
pat.not.found.message=Personal Access Token not found
```

---

## Usage Examples

### Example 1: Create a Token with Expiration

**Request**:
```http
POST /rest/users/personal-access-tokens HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "name": "CI/CD Pipeline Token",
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

**Response**:
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "publicId": "x1Y2z3A4b5C6d7E8",
  "name": "CI/CD Pipeline Token",
  "loginName": "jenkins",
  "token": "openl_pat_x1Y2z3A4b5C6d7E8.f9G0h1I2j3K4l5M6n7O8p9Q0r1S2t3U4v5W6",
  "createdAt": "2025-12-23T10:30:00Z",
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

**Important**: Save the token immediately! It will never be shown again.

---

### Example 2: Create a Token Without Expiration

**Request**:
```http
POST /rest/users/personal-access-tokens HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "name": "Development Token",
  "expiresAt": null
}
```

**Response**:
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "publicId": "m1N2o3P4q5R6s7T8",
  "name": "Development Token",
  "loginName": "developer",
  "token": "openl_pat_m1N2o3P4q5R6s7T8.u9V0w1X2y3Z4a5B6c7D8e9F0g1H2i3J4k5L6",
  "createdAt": "2025-12-23T10:30:00Z",
  "expiresAt": null
}
```

---

### Example 3: List All Tokens

**Request**:
```http
GET /rest/users/personal-access-tokens HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "publicId": "x1Y2z3A4b5C6d7E8",
    "name": "CI/CD Pipeline Token",
    "loginName": "jenkins",
    "createdAt": "2025-12-23T10:30:00Z",
    "expiresAt": "2026-12-31T23:59:59Z"
  },
  {
    "publicId": "m1N2o3P4q5R6s7T8",
    "name": "Development Token",
    "loginName": "jenkins",
    "createdAt": "2025-12-23T11:00:00Z",
    "expiresAt": null
  }
]
```

---

### Example 4: Use PAT for API Access

**Request**:
```http
GET /rest/design/list HTTP/1.1
Host: localhost:8080
Authorization: Token openl_pat_x1Y2z3A4b5C6d7E8.f9G0h1I2j3K4l5M6n7O8p9Q0r1S2t3U4v5W6
```

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "name": "project1",
    "path": "design/project1"
  }
]
```

---

### Example 5: Delete a Token

**Request**:
```http
DELETE /rest/users/personal-access-tokens/x1Y2z3A4b5C6d7E8 HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**:
```http
HTTP/1.1 204 No Content
```

---

### Example 6: Error - Duplicate Name

**Request**:
```http
POST /rest/users/personal-access-tokens HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "name": "CI/CD Pipeline Token",
  "expiresAt": null
}
```

**Response**:
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "code": "pat.duplicate.name.message",
  "message": "A token with this name already exists"
}
```

---

### Example 7: Error - PAT Cannot Manage PATs

**Request**:
```http
POST /rest/users/personal-access-tokens HTTP/1.1
Host: localhost:8080
Authorization: Token openl_pat_x1Y2z3A4b5C6d7E8.f9G0h1I2j3K4l5M6n7O8p9Q0r1S2t3U4v5W6
Content-Type: application/json

{
  "name": "Another Token",
  "expiresAt": null
}
```

**Response**:
```http
HTTP/1.1 403 Forbidden
Content-Type: application/json

{
  "message": "Access Denied"
}
```

---

## Security Considerations

### Token Storage

1. **Client-Side Storage**:
   - Store tokens securely (e.g., environment variables, secret managers)
   - **Never** commit tokens to version control
   - **Never** log token values
   - Use secure transmission (HTTPS only)

2. **Server-Side Storage**:
   - Secrets are hashed using `PasswordEncoder` (BCrypt by default)
   - Only hashes are stored in the database
   - Public IDs are indexed for fast lookups

### Token Generation

- **Public ID**: 16 characters (Base62) = ~95 bits of entropy
- **Secret**: 32 characters (Base62) = ~190 bits of entropy
- **Collision Prevention**: Automatic retry on public ID collision (extremely rare)
- **Cryptographic Strength**: Uses `SecureRandom` for generation

### Validation Security

The PAT validation process includes multiple security measures:

1. **Timing Attack Prevention**:
   - Constant-time password comparison
   - Dummy hash used for non-existent tokens
   - No information disclosure on failure reasons

2. **Format Validation**:
   - Token length limits (max 256 chars to prevent DoS)
   - Base62 character set validation
   - Fixed component lengths (public ID: 16, secret: 32)

3. **Expiration Handling**:
   - Server-side clock used for consistency
   - Expired tokens fail silently (no info disclosure)

### Access Control

1. **Endpoint Protection**:
   - All endpoints require OAuth2/SAML authentication
   - `@NotPatAuth` prevents PATs from managing PATs
   - User isolation: users can only access their own tokens

2. **Database Constraints**:
   - Foreign key to `OpenL_Users` with `ON DELETE CASCADE`
   - Unique constraint on `(loginName, name)` pair
   - Check constraint: `expiresAt > createdAt`

### Best Practices

1. **Token Lifecycle**:
   - Use expiration dates for temporary access
   - Rotate tokens periodically
   - Delete unused tokens immediately

2. **Scope Limitation**:
   - Create tokens with specific purposes (descriptive names)
   - Use separate tokens for different integrations
   - Revoke tokens when no longer needed

3. **Incident Response**:
   - If a token is compromised, delete it immediately
   - User account deletion cascades to all tokens

---

## Implementation Details

### Technology Stack

- **Framework**: Spring Boot 3.x
- **Security**: Spring Security 6.x
- **Validation**: Jakarta Validation (Hibernate Validator)
- **Database**: JPA/Hibernate with Flyway migrations
- **API Documentation**: OpenAPI 3.0 (Swagger)

### Database Schema

```sql
CREATE TABLE OpenL_PAT_Tokens (
    publicId   VARCHAR(16) NOT NULL PRIMARY KEY,
    secretHash VARCHAR(255) NOT NULL,
    createdAt  TIMESTAMP NOT NULL,
    expiresAt  TIMESTAMP,
    loginName  VARCHAR(50) NOT NULL,
    name       VARCHAR(100) NOT NULL,

    CONSTRAINT fk_OpenL_PAT_Tokens_user
        FOREIGN KEY (loginName)
        REFERENCES OpenL_Users(loginName)
        ON DELETE CASCADE,

    CONSTRAINT uq_OpenL_PAT_Tokens_user_name
        UNIQUE (loginName, name),

    CONSTRAINT ck_OpenL_PAT_Tokens_expires
        CHECK (expiresAt IS NULL OR expiresAt > createdAt)
);

CREATE INDEX ix_OpenL_PAT_Tokens_loginName
    ON OpenL_PAT_Tokens (loginName);
```

### Configuration

PAT endpoints are conditionally enabled based on user mode:

```java
@ConditionalOnExpression("'${user.mode}' == 'oauth2' || '${user.mode}' == 'saml'")
```

Supported user modes: `oauth2`, `saml`

---

## Related Documentation

- [Personal Access Token Architecture](./personal-access-token-architecture.md) - Implementation details and design decisions
- [API Guide](../API_GUIDE.md) - General API usage guidelines
- [Security Guide](../developer-guide/security.md) - OpenL Tablets security architecture
- [OAuth2 Configuration](../configuration/oauth2.md) - Setting up OAuth2 authentication

---

## Changelog

### Version 6.0.0 (EPBDS-15458)

- Initial implementation of Personal Access Token API
- Support for token creation, listing, retrieval, and deletion
- OAuth2/SAML authentication required
- Base62 token encoding with cryptographic security
- Optional token expiration support
- Comprehensive security measures against timing attacks and injection
- Integration with Spring Security filter chain

---

## Support

For issues or questions:

- **Bug Reports**: [GitHub Issues](https://github.com/openl-tablets/openl-tablets/issues)
- **Documentation**: [OpenL Tablets Docs](https://openl-tablets.org/documentation)
- **Community**: [OpenL Tablets Forum](https://openl-tablets.org/forum)

---

**Document Version**: 1.0
**Last Updated**: 2025-12-23
**Maintained By**: OpenL Tablets Development Team
