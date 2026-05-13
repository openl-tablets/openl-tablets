#### Configuring SSO: SAML Mode

When **SSO: SAML** is selected, configure the connection to the SAML identity provider:

| Field                          | Description                                                                                          |
|--------------------------------|------------------------------------------------------------------------------------------------------|
| **Entity ID**                  | A globally unique identifier that represents OpenL Studio as a SAML service provider. This value must be registered with the identity provider.  |
| **Server Metadata URL**        | URL to the identity provider's SAML metadata document, used to automatically configure the trust relationship between OpenL Studio and the identity provider. |
| **Remote Server Certificate**  | The identity provider's public X.509 certificate, used to verify the signature of SAML responses. Required when the metadata URL is not publicly accessible or does not include the certificate. |
| **Attribute for Username**     | Name of the SAML attribute that contains the username.                                     |
| **Attribute for First Name**   | Name of the SAML attribute that contains the user's first name.                            |
| **Attribute for Last Name**    | Name of the SAML attribute that contains the user's last name.                             |
| **Attribute for Display Name** | Name of the SAML attribute that contains the display name.                                 |
| **Attribute for Email**        | Name of the SAML attribute that contains the user's email address.                         |
| **Attribute for Groups**       | Name of the SAML attribute that contains the user's group memberships.                     |

Proceed to [Configuring Initial Users](#configuring-initial-users) to set up the administrator account and the default group.
