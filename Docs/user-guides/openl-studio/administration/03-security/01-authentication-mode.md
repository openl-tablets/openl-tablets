#### Selecting an Authentication Mode

OpenL Studio supports the following authentication modes:

| Mode                        | Description                                                                                                                                     |
|-----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| **Single-User**             | Only one user can run OpenL Studio. No login is required. Suitable for development and local evaluation only.                                   |
| **Multi-User**              | Multiple users can run OpenL Studio using unique usernames. User credentials are managed directly in OpenL Studio. Suitable for teams without an external identity provider. |
| **Active Directory / LDAP** | Multiple users authenticate against a corporate Active Directory or LDAP server. User credentials are managed by the directory service. External users are created and synchronized from the directory at login. |
| **SSO: SAML**               | Single Sign-On using the SAML 2.0 protocol. Works with identity providers such as Okta, Azure AD, and similar services.                         |
| **SSO: OIDC (OAuth2)**      | Single Sign-On using the OAuth2 / OpenID Connect protocol. Supports providers such as Google, GitHub, and others.                               |

**Note:** CAS authentication is no longer supported starting with OpenL Tablets 6.0.
