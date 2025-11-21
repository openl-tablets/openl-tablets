# 🧪 WebStudio Sample Environment

This sample Docker Compose setup runs a fully integrated environment for **WebStudio**, preconfigured to connect to a Git repository and PostgreSQL database. It includes:

- [Gitea](https://gitea.io) – a lightweight Git server
- WebStudio – for rule editing and testing
- PostgreSQL – for storing WebStudio metadata
- Init Containers – to bootstrap Gitea and database schemas

---

## What’s Included

### Services

| Service     | Purpose                                                         |
|-------------|-----------------------------------------------------------------|
| `gitea`     | Lightweight Git server for storing rules and metadata          |
| `gitea-init`| Initializes the Gitea instance, creates user and repository    |
| `studio` | WebStudio, preconfigured to connect to Gitea and Postgres|
| `postgres`  | PostgreSQL database used by Studio                          |
| `init`      | One-time container to download JDBC driver and setup schemas   |

---

## Features

- **Gitea pre-seeded with admin user and empty repo**
- **WebStudio configured for Git-based rule projects**
- **PostgreSQL database with prepared schemas**
- **Support for protected branches and structured commits**

---

## Security Notice

This sample setup uses basic built-in authentication for demonstration purposes:
```commandline
USER_MODE=multi
SECURITY_ADMINISTRATORS=admin
```
Multi-user mode allows login using predefined WebStudio accounts.

The default admin user has full access to the system. Default credentials are admin/admin

Other options are described below.

---

## Getting Started

### Prerequisites

- Docker CLI with the Docker Compose plugin
- A containerization service
- Internet connection (required to download base images and JDBC driver)

### Start the environment

```bash
docker compose up
```

This will:

- Start all services
- Initialize the Git repository and Gitea user
- Set up WebStudio with database and Git connectivity

---

## Access Information

| Service      | URL / Host              | Notes                                                     |
|--------------|-------------------------|-----------------------------------------------------------|
| Gitea        | http://localhost:3000   | Login: `admin_user` / `admin_password`                    |
| WebStudio | http://localhost:8081   | Login: `admin` / `admin` (multi-user mode enabled)        |
| PostgreSQL   | localhost:5432          | DB: `openl_studio`, User: `openl_user` / `openl_password` |

> Do not use default passwords in production. Update `compose.yaml` as needed.

---

## WebStudio Git Repository Configuration

This setup defines a repository named `example` in WebStudio using environment variables. It enforces:

- A specific commit message format (e.g., `ABC-123: short message`)
- Protected branches (`main`, `release-*`)
- Git commit attribution with a named user and email

For a detailed explanation of configuration parameters, refer to the [WebStudio Git Integration Guide](https://openldocs.readthedocs.io/en/latest/documentation/guides/webstudio_user_guide/#setting-up-a-connection-to-a-git-repository).

---

## Use Cases

- Local testing of rule development lifecycle with Git
- Evaluating multi-user WebStudio functionality
- Integrating WebStudio with CI/CD pipelines
- Serving as a base for custom OpenL Rule Services platforms

---

## Different security options
There are several other options for user management other than default multi-user. There is an example for Active Directory below. Please be aware that you have to configure identity provider by yourself before starting this config.

```bash
docker compose -f compose.yaml -f compose.ad.yaml up
```

There are two more possible **USER_MODE** value: **oauth2** and **saml**. 

For all possible values please refer to [default properties file](/STUDIO/org.openl.rules.webstudio/resources/openl-default.properties). Corresponding properties could be found under **security.<user-mode>.*** names. Please use [Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config) configuration externalization practices.


---

## Cleanup

To remove all containers and volumes:

```bash
docker compose down -v
```