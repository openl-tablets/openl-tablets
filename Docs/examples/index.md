# Examples

Working configurations and projects for deploying OpenL Tablets. They are kept next to the documentation, so a copy is
a start for an own setup.

## Available Examples

- [Production Deployment Examples](production/README.md) — what the repository holds for a production setup:
    - **`studio-config/`** — Docker Compose files of OpenL Studio with PostgreSQL, a Git repository, and optionally
      Active Directory.
    - **`example/`** — a multi-module Maven build of OpenL Rule Services: a simple project, projects with a dependency,
      an authentication extension, and an application that runs them.
- [Kubernetes Example](k8s/README.md) — OpenL Studio in multi-user mode on Kubernetes with PostgreSQL.

## Related Documentation

- [Deployment](../DEPLOYMENT.md) — the Docker images and Kubernetes.
- [Production Deployment Guide](../Production_Deployment.md) — the roles and the stages of a production pipeline.
- [Configuration](../configuration/index.md) — where the settings are described.
- [Developer Guide](../developer-guides/index.md) — rules projects and the language.
