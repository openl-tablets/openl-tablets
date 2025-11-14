## Docker Support

Official Docker images are available for quick deployment.

### Available Images

- **OpenL Rule Services**: Production-ready rules execution
- **OpenL Studio**: Rule development environment
- **OpenL Demo**: Complete demo environment

### Docker Hub

Access images at: [hub.docker.com/r/openltablets/](https://hub.docker.com/r/openltablets/)

### Quick Start

```bash
# Run OpenL Studio
docker run -p 8080:8080 openltablets/studio

# Run Rule Services
docker run -p 8080:8080 openltablets/ruleservice

# Run Demo Environment
docker run -p 8080:8080 openltablets/demo
```

### Docker Compose

For complete setup, see [Docker Guide](../../operations/docker-guide.md).

---

