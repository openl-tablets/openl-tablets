#### Configuring SSO: OIDC (OAuth2) Mode

When **SSO: OIDC (OAuth2)** is selected, configure the connection to the OAuth2 / OpenID Connect identity provider:

| Field                          | Description                                                                                          |
|--------------------------------|------------------------------------------------------------------------------------------------------|
| **Client ID**                  | A public identifier for OpenL Studio as a registered application in the identity provider. Obtained when registering the application with the identity provider. |
| **Issuer URI**                 | The base URL of the identity provider's OpenID Connect discovery endpoint, for example, `https://accounts.google.com`. Used to automatically retrieve provider configuration. |
| **Client Secret**              | A confidential credential issued by the identity provider to authenticate OpenL Studio as a registered application. Must be kept secure and never shared publicly. |
| **Scope**                      | Space-separated list of OAuth2 scopes that define what user information OpenL Studio requests from the identity provider, for example, `openid profile email`. |
| **Attribute for Username**     | Name of the token claim that contains the username.                                        |
| **Attribute for First Name**   | Name of the token claim that contains the user's first name.                               |
| **Attribute for Last Name**    | Name of the token claim that contains the user's last name.                                |
| **Attribute for Display Name** | Name of the token claim that contains the display name.                                    |
| **Attribute for Email**        | Name of the token claim that contains the user's email address.                            |
| **Attribute for Groups**       | Name of the token claim that contains the user's group memberships.                        |

Proceed to [Configuring Initial Users](#configuring-initial-users) to set up the administrator account and the default group.
