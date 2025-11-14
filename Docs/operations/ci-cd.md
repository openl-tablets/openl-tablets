# OpenL Tablets CI/CD Pipeline

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT
**Platform**: GitHub Actions

---

## Table of Contents

- [Overview](#overview)
- [GitHub Actions Workflows](#github-actions-workflows)
- [Build Pipeline](#build-pipeline)
- [Docker Image Build](#docker-image-build)
- [Release Process](#release-process)
- [Security Scanning](#security-scanning)
- [Deployment Pipeline](#deployment-pipeline)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

---

## Overview

OpenL Tablets uses **GitHub Actions** for continuous integration and deployment.

**Workflow Location**: `.github/workflows/`

### CI/CD Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    GitHub Repository                     │
└────────────────┬────────────────────────────────────────┘
                 │
                 ├─ Push/PR ──────► Build Workflow
                 │                   ├─ Compile
                 │                   ├─ Test
                 │                   ├─ Package
                 │                   └─ Upload Artifacts
                 │
                 ├─ Manual ────────► Docker Workflow
                 │                   ├─ Build Image
                 │                   ├─ Tag (latest + version)
                 │                   └─ Push to DockerHub
                 │
                 ├─ Manual ────────► Release Workflow
                 │                   ├─ Maven Release
                 │                   ├─ GPG Sign
                 │                   └─ Deploy to Maven Central
                 │
                 ├─ Scheduled ─────► Security Scan (Trivy)
                 │                   └─ Vulnerability Report
                 │
                 └─ Manual ────────► Deployment
                                     └─ Deploy to Environment
```

---

## GitHub Actions Workflows

### Workflow Files

| Workflow | File | Trigger | Purpose |
|----------|------|---------|---------|
| **Build** | `build.yml` | Weekly + Manual | Full build and test |
| **Quick Build** | `build-quick.yml` | Manual | Fast build without tests |
| **Docker** | `docker.yml` | Manual | Build and push Docker images |
| **Release** | `release.yml` | Manual | Maven release to Central |
| **Beta Release** | `release-beta.yml` | Manual | Beta release |
| **Deploy** | `deploy.yml` | Manual | Deployment automation |
| **Security Scan** | `trivy.yml` | Scheduled | Vulnerability scanning |

### Workflow Overview

```yaml
# .github/workflows/build.yml (simplified)
name: Build

on:
  schedule:
    - cron: '0 2 * * 3'  # Weekly on Wednesday at 02:00 UTC
  workflow_dispatch:      # Manual trigger

jobs:
  build:
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        java: [21, 25]

    runs-on: ${{ matrix.os }}

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
      - run: mvn clean verify
```

---

## Build Pipeline

### Build Workflow (build.yml)

**Full Path**: `.github/workflows/build.yml`

#### Trigger Conditions

1. **Scheduled**: Weekly on Wednesday at 02:00 UTC
2. **Manual**: Via `workflow_dispatch`

#### Build Matrix

| Parameter | Values | Purpose |
|-----------|--------|---------|
| **OS** | `ubuntu-latest`, `windows-latest`, `macos-latest` | Cross-platform testing |
| **Java** | `21`, `25` | Multi-version compatibility |
| **Docker** | `true` (Linux only), `false` (Windows/macOS) | Platform-specific tests |

#### Build Steps

```yaml
jobs:
  build:
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        java: [21, 25]

    steps:
      # 1. Checkout code
      - name: Checkout repository
        uses: actions/checkout@v4

      # 2. Setup Java
      - name: Setup Java ${{ matrix.java }}
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: ${{ matrix.java }}
          cache: 'maven'

      # 3. Build and test
      - name: Build with Maven
        run: mvn clean verify -B

      # 4. Upload test reports
      - name: Upload test results
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: test-results-${{ matrix.os }}-java-${{ matrix.java }}
          path: '**/target/surefire-reports/'

      # 5. Upload artifacts
      - name: Upload build artifacts
        if: matrix.os == 'ubuntu-latest' && matrix.java == '21'
        uses: actions/upload-artifact@v3
        with:
          name: openl-artifacts
          path: |
            **/target/*.war
            **/target/*.jar
```

### Quick Build Workflow (build-quick.yml)

**Purpose**: Fast feedback without full test suite

```yaml
name: Quick Build

on:
  workflow_dispatch:

jobs:
  quick-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: 21
      - run: mvn clean install -DskipTests -B
```

**Use Case**: Quick compilation check without waiting for tests

---

## Docker Image Build

### Docker Workflow (docker.yml)

**Full Path**: `.github/workflows/docker.yml`

#### Trigger

- **Manual only**: `workflow_dispatch` with version input

#### Workflow Steps

```yaml
name: Docker Build and Push

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version tag (e.g., 6.0.0)'
        required: true

jobs:
  docker:
    runs-on: ubuntu-latest

    steps:
      # 1. Checkout
      - name: Checkout
        uses: actions/checkout@v4

      # 2. Docker meta (tags)
      - name: Docker meta
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: openltablets/openl-tablets
          tags: |
            type=raw,value=latest
            type=raw,value=${{ github.event.inputs.version }}

      # 3. Set up Docker Buildx
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      # 4. Login to DockerHub
      - name: Login to DockerHub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      # 5. Build and push
      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=registry,ref=openltablets/openl-tablets:buildcache
          cache-to: type=registry,ref=openltablets/openl-tablets:buildcache,mode=max
```

### Image Tags

| Tag | Description | Example |
|-----|-------------|---------|
| `latest` | Latest stable release | `openltablets/openl-tablets:latest` |
| `<version>` | Specific version | `openltablets/openl-tablets:6.0.0` |
| `<version>-SNAPSHOT` | Snapshot build | `openltablets/openl-tablets:6.0.0-SNAPSHOT` |

### Manual Docker Build

```bash
# Build image locally
docker build -t openltablets/openl-tablets:6.0.0 .

# Tag as latest
docker tag openltablets/openl-tablets:6.0.0 openltablets/openl-tablets:latest

# Push to DockerHub
docker push openltablets/openl-tablets:6.0.0
docker push openltablets/openl-tablets:latest
```

---

## Release Process

### Release Workflow (release.yml)

**Full Path**: `.github/workflows/release.yml`

#### Prerequisites

1. **GPG Key**: For artifact signing
2. **Maven Credentials**: For Central deployment
3. **Git Credentials**: For tagging

#### Release Steps

```yaml
name: Maven Release

on:
  workflow_dispatch:
    inputs:
      release-version:
        description: 'Release version (e.g., 6.0.0)'
        required: true
      next-version:
        description: 'Next development version (e.g., 6.1.0-SNAPSHOT)'
        required: true

jobs:
  release:
    runs-on: ubuntu-latest

    steps:
      # 1. Checkout
      - name: Checkout
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      # 2. Setup Java
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: 'temurin'
          cache: 'maven'
          server-id: ossrh
          server-username: MAVEN_USERNAME
          server-password: MAVEN_PASSWORD

      # 3. Import GPG key
      - name: Import GPG key
        run: |
          echo "${{ secrets.GPG_PRIVATE_KEY }}" | gpg --batch --import
          gpg --list-secret-keys

      # 4. Configure Git
      - name: Configure Git
        run: |
          git config user.name "GitHub Actions"
          git config user.email "actions@github.com"

      # 5. Maven release
      - name: Maven Release
        env:
          MAVEN_USERNAME: ${{ secrets.OSSRH_USERNAME }}
          MAVEN_PASSWORD: ${{ secrets.OSSRH_TOKEN }}
          GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
        run: |
          mvn release:prepare \
            -DreleaseVersion=${{ github.event.inputs.release-version }} \
            -DdevelopmentVersion=${{ github.event.inputs.next-version }} \
            -Psources,gpg-sign \
            -B

          mvn release:perform \
            -Psources,gpg-sign \
            -B

      # 6. Create GitHub Release
      - name: Create GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          tag_name: v${{ github.event.inputs.release-version }}
          name: OpenL Tablets ${{ github.event.inputs.release-version }}
          draft: false
          prerelease: false
```

### Manual Release Process

```bash
# 1. Prepare release
mvn release:prepare \
  -DreleaseVersion=6.0.0 \
  -DdevelopmentVersion=6.1.0-SNAPSHOT \
  -Psources,gpg-sign

# 2. Perform release
mvn release:perform \
  -Psources,gpg-sign

# 3. Push tags
git push origin v6.0.0
```

### Release Profiles

**Maven profiles used in release**:

```xml
<!-- pom.xml -->
<profiles>
  <!-- GPG Signing -->
  <profile>
    <id>gpg-sign</id>
    <build>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-gpg-plugin</artifactId>
          <executions>
            <execution>
              <id>sign-artifacts</id>
              <phase>verify</phase>
              <goals>
                <goal>sign</goal>
              </goals>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </build>
  </profile>

  <!-- Source and Javadoc -->
  <profile>
    <id>sources</id>
    <build>
      <plugins>
        <plugin>
          <artifactId>maven-source-plugin</artifactId>
          <executions>
            <execution>
              <id>attach-sources</id>
              <goals>
                <goal>jar-no-fork</goal>
              </goals>
            </execution>
          </executions>
        </plugin>
        <plugin>
          <artifactId>maven-javadoc-plugin</artifactId>
          <executions>
            <execution>
              <id>attach-javadocs</id>
              <goals>
                <goal>jar</goal>
              </goals>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </build>
  </profile>
</profiles>
```

---

## Security Scanning

### Trivy Workflow (trivy.yml)

**Full Path**: `.github/workflows/trivy.yml`

#### Purpose

Scan Docker images for vulnerabilities

```yaml
name: Trivy Security Scan

on:
  schedule:
    - cron: '0 3 * * *'  # Daily at 03:00 UTC
  workflow_dispatch:

jobs:
  trivy:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      # Scan Dockerfile
      - name: Run Trivy on Dockerfile
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'config'
          scan-ref: '.'
          exit-code: '1'
          severity: 'CRITICAL,HIGH'

      # Scan Docker image
      - name: Build image for scanning
        run: docker build -t openl-tablets:test .

      - name: Run Trivy on image
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: 'openl-tablets:test'
          format: 'sarif'
          output: 'trivy-results.sarif'

      # Upload results to GitHub Security
      - name: Upload Trivy results
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: 'trivy-results.sarif'
```

### Manual Security Scan

```bash
# Scan Dockerfile
trivy config .

# Scan Docker image
docker build -t openl-tablets:test .
trivy image openl-tablets:test

# Scan with severity filter
trivy image --severity HIGH,CRITICAL openl-tablets:test

# Generate report
trivy image --format json --output report.json openl-tablets:test
```

---

## Deployment Pipeline

### Deployment Workflow (deploy.yml)

**Full Path**: `.github/workflows/deploy.yml`

```yaml
name: Deploy

on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Target environment'
        required: true
        type: choice
        options:
          - staging
          - production

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: ${{ github.event.inputs.environment }}

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Deploy to ${{ github.event.inputs.environment }}
        env:
          DEPLOY_KEY: ${{ secrets.DEPLOY_KEY }}
          DEPLOY_HOST: ${{ secrets.DEPLOY_HOST }}
        run: |
          # Deploy script
          ./scripts/deploy.sh ${{ github.event.inputs.environment }}
```

### Deployment Strategies

#### 1. Docker Deployment

```bash
#!/bin/bash
# scripts/deploy.sh

ENVIRONMENT=$1
VERSION=$2

# Pull latest image
docker pull openltablets/openl-tablets:${VERSION}

# Stop old container
docker stop openl-tablets || true
docker rm openl-tablets || true

# Start new container
docker run -d \
  --name openl-tablets \
  -p 8080:8080 \
  -v /data/openl:/opt/openl/shared \
  -e JAVA_OPTS="-Xmx2g" \
  openltablets/openl-tablets:${VERSION}
```

#### 2. Kubernetes Deployment

```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: openl-tablets
spec:
  replicas: 3
  selector:
    matchLabels:
      app: openl-tablets
  template:
    metadata:
      labels:
        app: openl-tablets
    spec:
      containers:
      - name: openl-tablets
        image: openltablets/openl-tablets:6.0.0
        ports:
        - containerPort: 8080
        env:
        - name: JAVA_OPTS
          value: "-XX:MaxRAMPercentage=90.0"
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
```

```bash
# Deploy to Kubernetes
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl rollout status deployment/openl-tablets
```

---

## Best Practices

### 1. Branch Protection

Configure branch protection rules:

```yaml
# .github/branch-protection.yml
master:
  required_status_checks:
    strict: true
    contexts:
      - build (ubuntu-latest, 21)
      - build (windows-latest, 21)
  required_pull_request_reviews:
    required_approving_review_count: 1
  enforce_admins: false
  restrictions: null
```

### 2. Secrets Management

Store sensitive data in GitHub Secrets:

- `DOCKERHUB_USERNAME`: DockerHub username
- `DOCKERHUB_TOKEN`: DockerHub access token
- `OSSRH_USERNAME`: Maven Central username
- `OSSRH_TOKEN`: Maven Central token
- `GPG_PRIVATE_KEY`: GPG private key for signing
- `GPG_PASSPHRASE`: GPG key passphrase
- `DEPLOY_KEY`: SSH key for deployment

### 3. Artifact Management

```yaml
# Upload artifacts with expiration
- name: Upload artifacts
  uses: actions/upload-artifact@v3
  with:
    name: build-artifacts
    path: target/*.war
    retention-days: 30  # Keep for 30 days
```

### 4. Cache Maven Dependencies

```yaml
- name: Setup Java
  uses: actions/setup-java@v4
  with:
    java-version: 21
    cache: 'maven'  # Cache Maven dependencies
```

### 5. Matrix Testing

Test across multiple configurations:

```yaml
strategy:
  matrix:
    os: [ubuntu-latest, windows-latest, macos-latest]
    java: [21, 25]
  fail-fast: false  # Continue even if one combination fails
```

### 6. Conditional Steps

```yaml
# Run only on main branch
- name: Deploy
  if: github.ref == 'refs/heads/main'
  run: ./deploy.sh

# Run only on Linux
- name: Docker tests
  if: runner.os == 'Linux'
  run: mvn verify -Pdocker-tests
```

---

## Troubleshooting

### Build Failures

#### 1. Out of Memory

**Problem**: Build fails with `OutOfMemoryError`

**Solution**:
```yaml
- name: Build with Maven
  env:
    MAVEN_OPTS: "-Xmx2g"
  run: mvn clean verify
```

#### 2. Test Failures

**Problem**: Tests fail intermittently

**Solution**:
```yaml
# Retry failed tests
- name: Run tests
  run: mvn test -Dsurefire.rerunFailingTestsCount=2
```

#### 3. Dependency Download Issues

**Problem**: Dependencies fail to download

**Solution**:
```yaml
# Use different Maven repository
- name: Build
  run: mvn clean verify -B -s settings.xml
```

```xml
<!-- settings.xml -->
<settings>
  <mirrors>
    <mirror>
      <id>central-mirror</id>
      <url>https://repo.maven.apache.org/maven2</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```

### Docker Build Issues

#### 1. Build Timeout

**Problem**: Docker build times out

**Solution**:
```yaml
- name: Build Docker image
  run: docker build --progress=plain --no-cache -t openl-tablets .
  timeout-minutes: 30
```

#### 2. Push Failed

**Problem**: Cannot push to DockerHub

**Solution**:
```bash
# Verify credentials
echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USERNAME" --password-stdin

# Retry push
docker push openltablets/openl-tablets:6.0.0
```

### Release Issues

#### 1. GPG Signing Failed

**Problem**: GPG signing fails in CI

**Solution**:
```yaml
- name: Import GPG key
  run: |
    echo "$GPG_PRIVATE_KEY" | gpg --batch --import
    echo "use-agent" >> ~/.gnupg/gpg.conf
    echo "pinentry-mode loopback" >> ~/.gnupg/gpg.conf
```

#### 2. Maven Release Failed

**Problem**: Release prepare/perform fails

**Solution**:
```bash
# Clean release
mvn release:clean

# Retry release
mvn release:prepare -DskipTests
mvn release:perform -DskipTests
```

---

## Workflow Monitoring

### View Workflow Runs

1. Navigate to **Actions** tab in GitHub repository
2. Select workflow from left sidebar
3. View individual runs and logs

### Download Artifacts

```bash
# Using GitHub CLI
gh run download <run-id>

# Or from web UI
# 1. Open workflow run
# 2. Scroll to "Artifacts" section
# 3. Click to download
```

### View Logs

```bash
# Using GitHub CLI
gh run view <run-id> --log

# Or from web UI
# 1. Open workflow run
# 2. Click on job
# 3. Expand step to view logs
```

---

## Integration with Other Tools

### Integrate with Slack

```yaml
- name: Notify Slack
  if: failure()
  uses: slackapi/slack-github-action@v1
  with:
    webhook-url: ${{ secrets.SLACK_WEBHOOK }}
    payload: |
      {
        "text": "Build failed: ${{ github.repository }}"
      }
```

### Integrate with JIRA

```yaml
- name: Update JIRA
  uses: atlassian/gajira-transition@v3
  with:
    issue: ${{ github.event.issue.number }}
    transition: "Done"
```

---

## Related Documentation

- [Testing Guide](../guides/testing-guide.md) - Test automation
- [Docker Guide](docker-guide.md) - Docker deployment
- [Performance Tuning](../guides/performance-tuning.md) - Build optimization
- [Troubleshooting](../onboarding/troubleshooting.md) - Common issues

---

**Last Updated**: 2025-11-05
**Maintainer**: OpenL Tablets Team
