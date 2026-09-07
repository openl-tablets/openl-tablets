## Configuration

OpenL Studio keeps every setting in a single property namespace. The same setting can be changed in two ways:

- in the **Administration** area of the running application;
- in an `application.properties` file, before or between restarts.

This page explains where configuration is read from, how the two ways interact, and which property backs each field
of the **Administration** area. What each setting actually means is described once, in
[Using Administration Tools][admin] — this page links to it instead of repeating it.

> [!Note]
> Earlier versions of OpenL Studio configured the first launch through an Installation Wizard. The wizard has been
> removed. Prepare a new instance with an `application.properties` file, then refine the settings in the
> **Administration** area.

The following topics are included in this chapter:

-   [Where Settings Come From](#where-settings-come-from)
-   [The Home Directory](#the-home-directory)
-   [Property Reference by Administration Section](#property-reference-by-administration-section)
-   [Settings With No UI Field](#settings-with-no-ui-field)
-   [Configuration Examples](#configuration-examples)
-   [Encrypting Passwords](#encrypting-passwords)
-   [Cluster Mode Configuration](#cluster-mode-configuration)

---

## Where Settings Come From

### Configuration Sources

Sources are listed from the highest priority to the lowest. A value found in a higher source wins.

1. Servlet context initialization parameters, for example, `<context-param>` in `web.xml`.
2. JNDI attributes under `java:comp/env`.
3. Java system properties, for example, `-Dopenl.home=/srv/openl`.
4. Operating system environment variables.
5. Settings saved in the **Administration** area.
6. `application.properties` files.
7. Built-in defaults shipped with the application.

> [!Note]
> Java system properties and environment variables have a higher priority than the **Administration** area. A setting
> defined there is reapplied on every startup, and the corresponding field in the UI becomes read-only. Use these
> sources for values that must not be changed at runtime, such as credentials injected by a container.

Settings saved in the **Administration** area are not written back to `application.properties`. They are stored
separately, in `${openl.home.shared}/<application-name>.properties` — for example, `webstudio.properties` — so an
`application.properties` file keeps only the values an administrator wrote by hand. Deleting that file is equivalent to
**Restore Defaults and Restart**.

### Environment Variables

An environment variable is recognized either as the exact property name or in upper case with dots and dashes replaced
by underscores:

```bash
export openl.home=/srv/openl
export OPENL_HOME=/srv/openl        # equivalent
export USER_MODE=multi              # sets user.mode
```

### File Locations

An `application.properties` file is searched in the following locations, from the lowest priority to the highest:

1. `classpath:` and `classpath:config/`
2. the working directory and its `conf/` and `config/` subdirectories
3. the user home directory

A file in the user home directory therefore overrides a file in the working directory. Files for an active Spring
profile, such as `application-prod.properties`, override all non-profile files.

To search other locations or file names, redefine `openl.config.location` and `openl.config.name`. Both must be passed
as a Java system property or an environment variable, because they are resolved before any file is read.

### The Generated Property Reference

A running instance publishes every known property with its effective default value at
`/webstudio/application.properties`, for example, <http://localhost:8080/webstudio/application.properties>. Most
properties also carry a description. Use it as the authoritative list for the installed version — it also includes
properties contributed by optional modules that this page does not cover.

---

## The Home Directory

An instance keeps its data in the directory named by `openl.home`, which defaults to `${user.home}/.openl`. On the
first launch the directory is created and populated automatically. Point it at a dedicated location rather than a
system drive:

```properties
openl.home = /srv/openl
```

Data that always belongs to this instance alone:

- **`cache/`** — compiled projects. Safe to delete; it is rebuilt on demand.
- **`repositories/`** — local repository data and clones of remote repositories.
- **`users-db/`** — the embedded H2 user database used by the default `db.url`.

Data that a cluster shares, written under `openl.home.shared` instead:

- **`user-workspace/`** — local copies of the projects opened by users, together with their project history.
- **`locks/`** — locks held on projects and settings.
- **`repositories/settings/`** — settings saved in the **Administration** area and the project index,
  `openl-projects.yaml`.
- **`<application-name>.properties`** — the values saved in the **Administration** area, for example,
  `webstudio.properties`.
- **`notification.txt`** — the message broadcast from [Managing Notifications][notifications].

`openl.home.shared` defaults to `openl.home`, so on a standalone instance both groups sit in the same directory. See
[Cluster Mode Configuration](#cluster-mode-configuration).

---

## Property Reference by Administration Section

### System

Described in [Managing System Settings][system].

| Section                | Field                                       | Property                     |
|------------------------|---------------------------------------------|------------------------------|
| Core                   | Dispatching Validation                      | `dispatching.validation`     |
| Core                   | Verify on Edit                              | `compile.auto`               |
| Testing                | Thread Number for Tests                     | `test.run.thread.count`      |
| History                | Maximum count of saved changes per user     | `project.history.count`      |
| Other                  | Update table properties                     | `update.system.properties`   |
| Other                  | Date Format                                 | `data.format.date`           |
| Other                  | Time Format                                 | `data.format.time`           |
| Database Configuration | Database URL                                | `db.url`                     |
| Database Configuration | Login                                       | `db.user`                    |
| Database Configuration | Password                                    | `db.password`                |
| Database Configuration | Maximum Pool Size                           | `db.maximumPoolSize`         |

The following related properties have no field in the UI:

- **`openl.home`** — working directory of the instance. The default value is `${user.home}/.openl`.
- **`openl.home.shared`** — directory shared between instances of a cluster. See
  [Cluster Mode Configuration](#cluster-mode-configuration).
- **`user.workspace.home`** — directory that stores local copies of the projects opened by users.
- **`data.format.datetime`** — composed of `data.format.date` and `data.format.time`.
- **`admin.notification-file`** — file that stores the message sent from [Managing Notifications][notifications].

### Security

Described in [Managing Security Settings][security].

The authentication mode is selected by `user.mode`:

| UI mode                 | `user.mode` value |
|-------------------------|-------------------|
| Single-User             | `single`          |
| Multi-User              | `multi`           |
| Active Directory / LDAP | `ad`              |
| SSO: SAML               | `saml`            |
| SSO: OIDC (OAuth2)      | `oauth2`          |

#### Single-User

Applies when `user.mode` is `single`. See [Configuring Single-User Mode][single-user].

| Field        | Property                       |
|--------------|--------------------------------|
| Username     | `security.single.username`     |
| Email        | `security.single.email`        |
| First Name   | `security.single.first-name`   |
| Last Name    | `security.single.last-name`    |
| Display Name | `security.single.display-name` |

#### Initial Users

Applies to all modes except `single`. See [Configuring Initial Users][initial-users].

| Field                                       | Property                                   |
|---------------------------------------------|--------------------------------------------|
| Administrators                              | `security.administrators`                  |
| Default Group                               | `security.default-group`                   |
| Permit creating and deleting projects       | `security.allow-project-create-delete`     |
| Allow Managers to bypass protected branches | `security.allow-bypass-protected-branches` |

#### Active Directory / LDAP

See [Configuring Active Directory / LDAP Mode][ldap].

| Field        | Property                     |
|--------------|------------------------------|
| Domain       | `security.ad.domain`         |
| Server URL   | `security.ad.server-url`     |
| User Filter  | `security.ad.search-filter`  |
| Group Filter | `security.ad.group-filter`   |

#### SSO: SAML

See [Configuring SSO: SAML Mode][saml].

| Field                      | Property                                  |
|----------------------------|-------------------------------------------|
| Entity ID                  | `security.saml.entity-id`                 |
| Server Metadata URL        | `security.saml.saml-server-metadata-url`  |
| Remote Server Certificate  | `security.saml.server-certificate`        |
| Attribute for Username     | `security.saml.attribute.username`        |
| Attribute for First Name   | `security.saml.attribute.first-name`      |
| Attribute for Last Name    | `security.saml.attribute.last-name`       |
| Attribute for Display Name | `security.saml.attribute.display-name`    |
| Attribute for Email        | `security.saml.attribute.email`           |
| Attribute for Groups       | `security.saml.attribute.groups`          |

The `security.saml.forceAuthN` property additionally controls whether the identity provider must force a user to
reauthenticate. All SAML attribute names are case-sensitive.

#### SSO: OIDC (OAuth2)

See [Configuring SSO: OIDC (OAuth2) Mode][oidc].

| Field                      | Property                                  |
|----------------------------|-------------------------------------------|
| Client ID                  | `security.oauth2.client-id`               |
| Client Secret              | `security.oauth2.client-secret`           |
| Issuer URI                 | `security.oauth2.issuer-uri`              |
| Scope                      | `security.oauth2.scope`                   |
| Attribute for Username     | `security.oauth2.attribute.username`      |
| Attribute for First Name   | `security.oauth2.attribute.first-name`    |
| Attribute for Last Name    | `security.oauth2.attribute.last-name`     |
| Attribute for Display Name | `security.oauth2.attribute.display-name`  |
| Attribute for Email        | `security.oauth2.attribute.email`         |
| Attribute for Groups       | `security.oauth2.attribute.groups`        |

All OAuth2 claim names are case-sensitive.

### Repositories

Described in [Managing Repository Settings][repositories].

Each repository has an identifier that is used as a property prefix:

- **`design-repository-configs`** — comma-separated identifiers of the repositories on the
  **Design Repositories** tab.
- **`production-repository-configs`** — comma-separated identifiers of the repositories on the
  **Deployment Repositories** tab.

Settings of a repository are then defined as `repository.<id>.*`. Identifiers are arbitrary; `design` and `production`
are the conventional defaults.

The **Type** field is set by `repository.<id>.$ref`, which points at the block of defaults for that type:

| Type                 | `repository.<id>.$ref` value |
|----------------------|------------------------------|
| Git                  | `repo-git`                   |
| Database JDBC        | `repo-jdbc`                  |
| Database JNDI        | `repo-jndi`                  |
| AWS S3               | `repo-aws-s3`                |
| Azure Blob Storage   | `repo-azure-blob`            |

A `$ref` sets up inheritance: any `repository.<id>.<suffix>` left undefined falls back to `<ref>.<suffix>`. Defining
`repository.design.$ref = repo-git` and then only `repository.design.uri` is enough — the remaining Git settings come
from the `repo-git.*` defaults. The same mechanism lets `repo-git.*` and `repo-default.*` set values shared by every
repository of a kind.

#### Settings Common to All Types

| Field             | Property suffix       |
|-------------------|-----------------------|
| Name              | `.name`               |
| Path              | `.base.path`          |
| Deployment branch | `.deploy-from-branch` |

The **Deployment branch** field applies to deployment repositories only: an empty value means **Any branch**, and
`MAIN_BRANCH` means **Main branch only**.

#### Git

See [Managing Git Repository Settings][git-repo].

| Field                              | Property suffix                   |
|------------------------------------|-----------------------------------|
| URL                                | `.uri`                            |
| Login                              | `.login`                          |
| Password                           | `.password`                       |
| Branch                             | `.branch`                         |
| Protected branches                 | `.protected-branches`             |
| Changes check interval             | `.listener-timer-period`          |
| Connection timeout                 | `.connection-timeout`             |
| Default branch name                | `.new-branch.pattern`             |
| Branch name pattern                | `.new-branch.regex`               |
| Invalid branch name message hint   | `.new-branch.regex-error`         |

Retry behavior after a failed authentication is controlled by `.failed-authentication-seconds` and
`.max-authentication-attempts`. The location of cloned remote repositories is controlled by
`repo-git.local-repositories-folder`, which has no field in the UI.

#### Database JDBC and Database JNDI

| Field    | Property suffix |
|----------|-----------------|
| URL      | `.uri`          |
| Login    | `.login`        |
| Password | `.password`     |

For **Database JNDI**, the URL is the datasource name in the JNDI context, such as `java:comp/env/jdbc/DB`.

The **Secure connection** check box has no property of its own. It is derived from **Login**: the check box appears
selected whenever a login is defined, so setting `.login` and `.password` is what enables a secure connection.

#### AWS S3

| Field                  | Property suffix          |
|------------------------|--------------------------|
| Service endpoint       | `.service-endpoint`      |
| Bucket name            | `.bucket-name`           |
| Region name            | `.region-name`           |
| Access key             | `.access-key`            |
| Secret key             | `.secret-key`            |
| Listener period        | `.listener-timer-period` |
| SSE algorithm          | `.sse-algorithm`         |

#### Azure Blob Storage

| Field           | Property suffix          |
|-----------------|--------------------------|
| URL             | `.uri`                   |
| Account name    | `.account-name`          |
| Account key     | `.account-key`           |
| Listener period | `.listener-timer-period` |

#### Commit Comments

Applies to Git design repositories. See [Customizing Git Commit Comments][git-comments].

| Field                     | Property suffix                                        |
|---------------------------|--------------------------------------------------------|
| Customize comments        | `.comment-template.use-custom-comments`                |
| User message pattern      | `.comment-template.comment-validation-pattern`         |
| Invalid user message hint | `.comment-template.invalid-comment-message`            |
| Save project              | `.comment-template.user-message.default.save`          |
| Create project            | `.comment-template.user-message.default.create`        |
| Copy project              | `.comment-template.user-message.default.copied-from`   |
| Restore from old version  | `.comment-template.user-message.default.restored-from` |

A Git repository stores the resulting comment directly as the commit message; there is no separate message template to
configure.

### Email Server

Described in [Managing Email Server Configuration][mail].

| Field    | Property        |
|----------|-----------------|
| URL      | `mail.url`      |
| Username | `mail.username` |
| Password | `mail.password` |

Email address verification is active while these three properties hold valid values.

---

## Settings With No UI Field

These are set in `application.properties` only.

### Cross-Origin Requests

Needed when a browser application served from another origin calls the OpenL Studio REST API. No value for
`cors.allowed.origins` means the API cannot be called cross-origin at all.

| Property                | Description                                                                    |
|-------------------------|--------------------------------------------------------------------------------|
| `cors.allowed.origins`  | Comma-separated allowed origins. An asterisk allows any origin.                |
| `cors.allowed.methods`  | Comma-separated allowed HTTP methods.                                          |
| `cors.allowed.headers`  | Comma-separated allowed request headers.                                       |
| `cors.preflight.maxage` | Seconds a browser may cache a pre-flight response. A negative value omits it.  |

```properties
cors.allowed.origins = https://apps.example.com
cors.allowed.methods = GET,OPTIONS,HEAD,PUT,POST
cors.allowed.headers = Content-Type,Accept,api_key,Authorization
cors.preflight.maxage = 7200
```

### Password Hashing

In Multi-User mode, `webstudio.bcrypt.strength` sets the bcrypt cost applied to stored passwords. Permitted values are
4 to 31, and the default is 10. Each increment doubles the hashing work, so raise it in small steps and measure login
time.

### Migration Attribution

When an upgrade requires OpenL Studio to change project files in a repository, the commits are attributed to this
identity:

```properties
migration.user.name = Studio Migration
migration.user.email = openltablets@eisgroup.com
```

Set them to a recognizable service identity so that such commits are distinguishable from users' commits in the
repository history.

### UI Extension

`webstudio.javascript.url` loads an external JavaScript file together with OpenL Studio, which is the extension point
for integrating an external tool:

```properties
webstudio.javascript.url = https://example.com/extension.js
```

---

## Configuration Examples

### Single-User Development Setup

A local instance with no login and a Git repository in the file system:

```properties
openl.home = /srv/openl

user.mode = single
security.single.username = developer
security.single.email = developer@example.com
security.single.display-name = Local Developer

design-repository-configs = design
repository.design.name = Design
repository.design.$ref = repo-git
repository.design.uri = ${openl.home}/repositories/design
```

### Team Setup with a Remote Git Repository

Database-backed accounts, an administrator, a read-only baseline group, and a remote Git design repository:

```properties
user.mode = multi
db.url = jdbc:postgresql://db.example.com:5432/openl_studio
db.user = openl
db.password = ENC(9J0k1LmNoPqRsTuVwXyZ==)
security.administrators = jsmith
security.default-group = Viewers

design-repository-configs = design
repository.design.name = Design
repository.design.$ref = repo-git
repository.design.uri = https://git.example.com/rules/design.git
repository.design.login = openl-service
repository.design.password = ENC(aBcDeFgHiJkLmNoPqRsT==)
repository.design.branch = main
repository.design.protected-branches = main, release/*
repository.design.base.path = rules/
repository.design.connection-timeout = 60
repository.design.listener-timer-period = 10
```

### Deployment Repository in AWS S3

```properties
production-repository-configs = production
repository.production.name = Deployment
repository.production.$ref = repo-aws-s3
repository.production.bucket-name = openl-rules-prod
repository.production.region-name = eu-central-1
repository.production.access-key = AKIAIOSFODNN7EXAMPLE
repository.production.secret-key = ENC(uVwXyZaBcDeFgHiJkLmN==)
repository.production.base.path = deploy/
repository.production.deploy-from-branch = MAIN_BRANCH
```

Leave the access and secret keys empty to let the AWS SDK resolve credentials from the environment, an instance
profile, or another standard location.

### Two Design Repositories

Identifiers are arbitrary, so several repositories of different types can coexist:

```properties
design-repository-configs = design, archive

repository.design.name = Design
repository.design.$ref = repo-git
repository.design.uri = https://git.example.com/rules/design.git

repository.archive.name = Archive
repository.archive.$ref = repo-jdbc
repository.archive.uri = jdbc:postgresql://db.example.com:5432/openl_archive
repository.archive.login = openl
repository.archive.password = ENC(kLmNoPqRsTuVwXyZaBcD==)
```

### Active Directory Authentication

```properties
user.mode = ad
security.ad.domain = example.com
security.ad.server-url = ldap://ad.example.com:3268
security.ad.search-filter = (&(objectClass=user)(userPrincipalName={0}))
security.ad.group-filter = (&(objectClass=group)(member:1.2.840.113556.1.4.1941:={2}))
security.administrators = jsmith
security.default-group = Viewers
```

### OIDC Authentication

```properties
user.mode = oauth2
security.oauth2.client-id = openl-studio
security.oauth2.client-secret = ENC(oPqRsTuVwXyZaBcDeFgH==)
security.oauth2.issuer-uri = https://login.example.com/realms/openl
security.oauth2.scope = openid,profile,email
security.oauth2.attribute.username = preferred_username
security.oauth2.attribute.groups = groups
security.administrators = jsmith
```

---

## Encrypting Passwords

Any property value, not only a password, can be stored encrypted. Define a master password in `secret.key` and wrap the
encrypted value in `ENC(...)`:

```properties
secret.key = MyMasterPassword
db.password = ENC(eNcoDedPa$$w0RD)
```

Keep `secret.key` out of `application.properties` in a shared environment — pass it as a Java system property or an
environment variable instead. Quote the value if it contains spaces or shell metacharacters, so that
`-Dsecret.key=...` reaches the application intact.

`secret.key` also protects the passwords that the **Administration** area saves. When a repository, database, or mail
password is entered in the UI, it is stored as `ENC(...)` in the settings file, encrypted with the current
`secret.key`.

> [!Note]
> Define `secret.key` before creating any connection that stores a password. Passwords saved while `secret.key` was
> blank are stored as plain text, and passwords saved under a different `secret.key` decode to an empty value once the
> key changes — the affected connections then fail to authenticate and their passwords have to be entered again.

To produce an encrypted value on Linux, substituting the `secret.key` value for `MyMasterPassword`:

```bash
echo -n "plain password" \
  | openssl aes-128-cbc \
    -K $(echo -n "MyMasterPassword" | sha1sum | awk '{ print substr($1, 1, 32) }') \
    -e \
    -iv 00000000000000000000000000000000 \
    -base64 \
  | awk '{ print "ENC("$1")" }'
```

The value passed to `-K` is the first 32 characters of the SHA-1 hash of `secret.key`. On macOS, use `shasum` instead
of `sha1sum`. The encoding cipher is configurable through `secret.cipher`.

---

## Cluster Mode Configuration

Several OpenL Studio instances can share a workload. Each instance keeps its own `openl.home` for local data and
reads shared state from `openl.home.shared`, which must resolve to the same mounted directory on every instance:

```properties
openl.home = /srv/openl
openl.home.shared = /mnt/openl-shared
```

`openl.home.shared` defaults to `openl.home`, which is why a standalone instance needs no extra configuration.

Configure it **before the first launch**. Setting it on an instance that already holds data leaves that data behind in
the old location and requires moving it manually.

### What Moves to the Shared Directory

- **Administration settings** — every instance reads the same `<application-name>.properties`, so a change applied in
  one instance's **Administration** area takes effect everywhere.
- **User workspace and project history** — users see the same open projects and history on whichever instance serves
  them.
- **Locks** — an instance sees projects locked by users on other instances.
- **Project index** and the broadcast notification.

### What Stays Local

The compiled-projects cache and the repository clones stay under `openl.home` on each instance.

> [!Note]
> The defaults for the user database (`db.url`) and for repositories also point inside `openl.home`, not inside
> `openl.home.shared`. Setting `openl.home.shared` alone therefore leaves every instance with its own users and its
> own project storage. A cluster must additionally point `db.url` at an external database and configure the design and
> deployment repositories to remote storage such as Git, a database, AWS S3, or Azure Blob Storage.

Put a load balancer in front of the instances and enable sticky sessions.

---

## Related Documentation

- [Using Administration Tools][admin] — what each setting means
- [Quick Start Installation](quick-start.md) — first installation steps
- [Rule Services](rule-services.md) — configuring Rule Services
- [Integration](integration.md) — connecting OpenL Studio with Rule Services
- [Troubleshooting](troubleshooting.md) — common installation issues

[admin]: ../openl-studio/administration/index.md#using-administration-tools
[system]: ../openl-studio/administration/02-system-settings.md#managing-system-settings
[notifications]: ../openl-studio/administration/05-notifications.md#managing-notifications
[security]: ../openl-studio/administration/03-security/index.md#managing-security-settings
[single-user]: ../openl-studio/administration/03-security/02-single-user.md#configuring-single-user-mode
[initial-users]: ../openl-studio/administration/03-security/07-initial-users.md#configuring-initial-users
[ldap]: ../openl-studio/administration/03-security/04-ldap.md#configuring-active-directory--ldap-mode
[saml]: ../openl-studio/administration/03-security/05-sso-saml.md#configuring-sso-saml-mode
[oidc]: ../openl-studio/administration/03-security/06-sso-oidc.md#configuring-sso-oidc-oauth2-mode
[repositories]: ../openl-studio/administration/01-repository-settings/index.md#managing-repository-settings
[git-repo]: ../openl-studio/administration/01-repository-settings/02-git-repository-settings.md#managing-git-repository-settings
[git-comments]: ../openl-studio/administration/01-repository-settings/02-git-repository-settings.md#customizing-git-commit-comments
[mail]: ../openl-studio/administration/07-email-server.md#managing-email-server-configuration
