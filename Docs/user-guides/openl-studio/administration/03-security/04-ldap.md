#### Configuring Active Directory / LDAP Mode

When **Active Directory / LDAP** is selected, configure the connection to the directory service:

| Field           | Description                                                                                                                                                                                                     |
|-----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Domain**      | The Active Directory domain name used for user authentication, for example, `example.com`. Appended to the username to form the `login@domain` format passed to the directory service.               |
| **Server URL**  | URL of the Active Directory or LDAP server, for example, `ldap://ad.example.com:389`.                                                                                                                |
| **User Filter** | LDAP filter string used to search for the authenticating user. <br/>`{0}` is replaced with `login@domain`. <br/>`{1}` is replaced with `login` only.                                                  |
| **Group Filter** | LDAP filter string used to search for the groups the user belongs to. <br/>`{0}` is replaced with `login@domain`. <br/>`{1}` is replaced with `login` only. <br/>`{2}` is replaced with the DN of the found user. |

Proceed to [Configuring Initial Users](#configuring-initial-users) to set up the administrator account and the default group.
