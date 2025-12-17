# 🚀 Quick Start: Running Everything Together

This guide will help you start OpenL Tablets and MCP server for working with Claude Desktop or Cursor IDE.

**Note**: This guide uses `$PROJECT_ROOT` to refer to the OpenL Tablets project directory. Set it before running commands:

```bash
export PROJECT_ROOT="/path/to/openl-tablets"
```

Or replace `$PROJECT_ROOT` with your actual project path in all commands below.

## 📋 What Needs to Be Started

1. **OpenL Tablets** - Rules server (port 8080)
2. **MCP Server** - Bridge between AI clients and OpenL (runs automatically)
3. **AI Client** - Claude Desktop or Cursor IDE

---

## 🎯 Method 1: Docker Compose (Recommended)

The easiest way is to run everything through Docker.

### Step 1: Start OpenL Tablets

```bash
cd $PROJECT_ROOT
docker compose up
```

This will start:
- PostgreSQL database
- OpenL Studio at `http://localhost:8080/studio`
- Rule Services at `http://localhost:8080/services`

**Wait 1-2 minutes** for everything to start. You'll see readiness messages in the logs.

### Step 2: Verify OpenL is Working

Open in browser: http://localhost:8080

You should see a page with links to Studio and Services.

**Login:**
- Username: `admin`
- Password: `admin`

### Step 3: Configure MCP Server

Follow the setup guide for your AI client:
- **Claude Desktop**: See [Claude Desktop Setup](../setup/CLAUDE-DESKTOP.md)
- **Cursor IDE**: See [Cursor Setup](../setup/CLAUDE-DESKTOP.md#cursor-ide-setup)
- **Cursor with Docker**: See [Cursor Docker Setup](../setup/CURSOR-DOCKER.md)

### Step 4: Test Connection

In your AI client chat, try:

```
List repositories in OpenL Tablets
```

The AI should use MCP tools and show the list of repositories.

---

## 🎯 Method 2: Local Run (without Docker)

If you don't have Docker or want to run locally.

### Step 1: Build OpenL Tablets

```bash
cd $PROJECT_ROOT
mvn clean install -DskipTests
```

This will take 10-30 minutes on first run.

### Step 2: Start OpenL via DEMO Script

```bash
cd DEMO
chmod +x start
./start
```

The script will automatically:
- Download Java (if needed)
- Download Jetty server
- Start OpenL Studio at `http://localhost:8080/webstudio`

### Step 3: Verify It Works

Open: http://localhost:8080/webstudio

**Login:**
- Username: `admin`
- Password: `admin`

### Step 4: Configure MCP Server

Follow the setup guide for your AI client (same as Step 3 in Method 1).

---

## 🔍 Health Check

### Automatic Check

```bash
cd $PROJECT_ROOT/mcp-server
export OPENL_BASE_URL="http://localhost:8080/rest"
export OPENL_USERNAME="admin"
export OPENL_PASSWORD="admin"
./check-health.sh
```

### Manual Check

1. **Is OpenL accessible?**
   ```bash
   curl http://localhost:8080/rest/projects
   ```
   Should return JSON or authentication error (not "connection refused")

2. **Is MCP server configured?**
   - Check configuration file for your AI client
   - See [Setup Guides](../setup/) for details

3. **Does AI client see the server?**
   - Open AI client settings
   - Find "MCP Servers" section
   - Should see `openl-mcp-server` with "Connected" status

---

## 🐛 Troubleshooting

### Issue: "Cannot connect to OpenL API"

**Solution:**
1. Ensure OpenL is running:
   ```bash
   curl http://localhost:8080/rest/projects
   ```
2. If Docker Compose:
   ```bash
   docker compose ps  # Check container status
   docker compose logs studio  # View logs
   ```
3. If local run - check that Jetty process is running

### Issue: MCP Server doesn't appear in AI client

**Solution:**
1. Check path in configuration (must be absolute)
2. Ensure project is built:
   ```bash
   cd mcp-server
   npm run build
   ls -la dist/index.js  # Should exist
   ```
3. Completely restart your AI client

### Issue: "Authentication failed"

**Solution:**
1. Check credentials in configuration
2. Try logging into OpenL via browser with the same credentials
3. Check URL - should end with `/rest`

### Issue: Docker containers don't start

**Solution:**
1. Check that Docker is running:
   ```bash
   docker ps
   ```
2. Check ports (8080, 5432 should not be occupied):
   ```bash
   lsof -i :8080
   lsof -i :5432
   ```
3. View logs:
   ```bash
   docker compose logs
   ```

For more detailed troubleshooting, see [Troubleshooting Guide](../guides/TROUBLESHOOTING.md).

---

## 📊 Startup Order (Brief Version)

```bash
# Terminal 1: Start OpenL Tablets
cd $PROJECT_ROOT
docker compose up

# Wait 1-2 minutes for everything to start

# Terminal 2: Check (optional)
cd mcp-server
export OPENL_BASE_URL="http://localhost:8080/rest"
export OPENL_USERNAME="admin"
export OPENL_PASSWORD="admin"
./check-health.sh

# Then:
# 1. Open http://localhost:8080 in browser
# 2. Login (admin/admin)
# 3. Configure MCP server (see Setup Guides)
# 4. Restart AI client
# 5. Check MCP server in settings
# 6. Try in AI client: "List repositories in OpenL Tablets"
```

---

## ✅ Readiness Checklist

- [ ] OpenL Tablets is running and accessible at http://localhost:8080
- [ ] Can log into OpenL Studio via browser (admin/admin)
- [ ] MCP server is configured in AI client configuration
- [ ] AI client restarted after configuration
- [ ] MCP server is visible in AI client settings
- [ ] AI client can execute command "List repositories"

---

## 📚 Next Steps

- [Configuration Guide](CONFIGURATION.md) - Detailed configuration options
- [Setup Guides](../setup/) - Client-specific setup instructions
- [Usage Examples](../guides/EXAMPLES.md) - Learn how to use MCP tools
- [Troubleshooting](../guides/TROUBLESHOOTING.md) - Common issues and solutions

---

## 💡 Useful Commands

```bash
# Stop Docker containers
docker compose down

# View OpenL logs
docker compose logs -f studio

# Rebuild MCP server
cd mcp-server
npm run build

# Check status of everything
cd mcp-server
./check-health.sh
```

