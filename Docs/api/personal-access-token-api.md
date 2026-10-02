# Personal Access Token (PAT) API

A personal access token (PAT) authenticates a program that calls the REST API of OpenL Studio on behalf of a user. The
program sends the token instead of the user's credentials, and the token can be revoked without changing the password.

The design of the feature is in [Personal Access Token Architecture](personal-access-token-architecture.md).

## Availability

- **User modes** — the PAT endpoints and the PAT authentication exist when `user.mode` is not `single`: `multi`, `ad`,
  `saml`, or `oauth2`. In `single` mode there are no users to own a token, and the endpoints do not exist.
- **Managing tokens** — the endpoints below accept the credentials that the configured user mode accepts for the REST API.
  A PAT cannot manage PATs: such a request answers `403 Forbidden`.
- **What a token can do** — a request with a PAT acts as its owner, with the permissions the owner has, except for the
  endpoints that need the administrator privilege. These answer `403 Forbidden`, even when the owner is an
  administrator.

## Token Format and Use

```
openl_pat_<publicId>.<secret>
```

- **`publicId`** — 16 characters of Base62 (`0-9`, `a-z`, `A-Z`). It identifies the token and is not secret.
- **`secret`** — 32 characters of Base62, generated with `SecureRandom`. Only a hash of it is stored.

A request presents the token with the `Token` scheme, not `Bearer`:

```http
GET /rest/projects HTTP/1.1
Authorization: Token openl_pat_a1B2c3D4e5F6g7H8.i9J0k1L2m3N4o5P6q7R8s9T0u1V2w3X4
```

A token that is malformed, unknown, expired, or of a disabled or locked user answers `401 Unauthorized`. The response
does not tell which of these it is.

## Endpoints

All the endpoints are under `/rest/users/personal-access-tokens`, and a user sees only the tokens of their own.

### Create a Token

```http
POST /rest/users/personal-access-tokens
Content-Type: application/json

{
  "name": "CI/CD Pipeline Token",
  "expiresAt": "2027-12-31T23:59:59Z"
}
```

- **`name`** — required, not blank, at most 100 characters, and unique among the tokens of the user.
- **`expiresAt`** — optional, an ISO 8601 instant in the future. Without it the token does not expire.

**Response** (`201 Created`):

```json
{
  "publicId": "a1B2c3D4e5F6g7H8",
  "name": "CI/CD Pipeline Token",
  "loginName": "jenkins",
  "token": "openl_pat_a1B2c3D4e5F6g7H8.i9J0k1L2m3N4o5P6q7R8s9T0u1V2w3X4",
  "createdAt": "2026-10-02T10:30:00Z",
  "expiresAt": "2027-12-31T23:59:59Z"
}
```

The `token` is shown only in this response. It cannot be read again; create a new token if it is lost.

**Errors:**

- `400 Bad Request` with `openl.error.400.pat.duplicate.name.message` — the user has a token with this name.
- `400 Bad Request` with a list of `fields` — the name is blank or longer than 100 characters.

### List the Tokens

```http
GET /rest/users/personal-access-tokens
```

**Response** (`200 OK`): a list of tokens without the `token` field.

```json
[
  {
    "publicId": "a1B2c3D4e5F6g7H8",
    "name": "CI/CD Pipeline Token",
    "loginName": "jenkins",
    "createdAt": "2026-10-02T10:30:00Z",
    "expiresAt": "2027-12-31T23:59:59Z"
  }
]
```

### Get a Token

```http
GET /rest/users/personal-access-tokens/{publicId}
```

**Response** (`200 OK`): one token without the `token` field, as in the list.

**Errors:** `404 Not Found` with `openl.error.404.pat.not.found.message` — there is no such token, or it belongs to
another user.

### Delete a Token

```http
DELETE /rest/users/personal-access-tokens/{publicId}
```

**Response:** `204 No Content`. The token stops working at once.

**Errors:** `404 Not Found` with `openl.error.404.pat.not.found.message`, as above.

## Errors

- **`401 Unauthorized`** — the request has no valid credentials.
- **`403 Forbidden`** with `{"message": "Access Denied"}` — the request is authenticated with a PAT, or the endpoint needs
  the administrator privilege.
- **`404 Not Found`** — the endpoint does not exist in `single` mode.

The error body is `{"code": "openl.error.<status>.<key>", "message": "..."}`; see [Errors](README.md#errors).

## Data Models

### CreatePersonalAccessTokenRequest

- `name` — the name of the token.
- `expiresAt` — the expiration instant, or absent.

### CreatedPersonalAccessTokenResponse

- `publicId`, `name`, `loginName`, `createdAt`, `expiresAt` — as in `PersonalAccessTokenResponse`.
- `token` — the full token. Present only in the response to the creation.

### PersonalAccessTokenResponse

- `publicId` — the public identifier of the token.
- `name` — the name of the token.
- `loginName` — the owner of the token.
- `createdAt` — when the token was created.
- `expiresAt` — when the token expires. Absent when it does not.

## Security Notes

- **Storage** — the database holds the public identifier and a hash of the secret. The hash is made by the password
  encoder that `security.password.encoder` selects, the same one that hashes the passwords of internal users. It is
  `bcrypt` by default. A token cannot be restored from the hash.
- **Comparison** — a request for an unknown `publicId` is checked against a placeholder hash, so the time of the answer
  does not tell whether the token exists.
- **Length** — a token longer than 256 characters is rejected before any lookup.
- **Removal** — deleting the user deletes the tokens of the user.
- **Client** — store the token in a secret manager, send it only over HTTPS, and keep it out of logs and version control.
