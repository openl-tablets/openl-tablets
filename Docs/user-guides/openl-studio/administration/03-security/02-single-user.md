#### Configuring Single-User Mode

When **Single-User** is selected, configure the single user account that will be used to run OpenL Studio:

| Field            | Description                                                                            |
|------------------|----------------------------------------------------------------------------------------|
| **Username**     | Login name used to identify the single user. Defaults to the operating system account. |
| **Email**        | Email address of the user. Used for Git commits and email verification.                |
| **First Name**   | User's first name. Used to form the display name and included in Git commit metadata.  |
| **Last Name**    | User's last name. Used to form the display name and included in Git commit metadata.   |
| **Display Name** | Full name shown in the OpenL Studio UI and recorded in Git commits.                    |

These settings provide the initial user data: the user account is created from them on the first start. After that, the
user data, except the username, is edited on the **My Profile** page in the same way as in other authentication modes.
If the email address or display name is empty, OpenL Studio asks for the missing details when the single user is
automatically signed in. First Name and Last Name are optional and can be used to generate the display name.

**Note:** A value defined by a more prioritized configuration source, such as a Java system property or an environment
variable, is applied on every startup and cannot be edited in the UI.
