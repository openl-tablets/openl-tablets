# 🚀 Quick Start: Running Everything Together

This guide will help you start OpenL Tablets and MCP server for working with Claude Desktop.

## 📋 What Needs to Be Started

1. **OpenL Tablets** - Rules server (port 8080)
2. **MCP Server** - Bridge between Claude Desktop and OpenL (runs automatically)
3. **Claude Desktop** - AI assistant application

---

## 🎯 Method 1: Docker Compose (Recommended)

The easiest way is to run everything through Docker.

### Step 1: Start OpenL Tablets

```bash
cd /Users/asamuseu/IdeaProjects/openl-tablets
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

### Step 3: Ensure MCP Server is Configured

Configuration is already copied to Claude Desktop. Check:

```bash
cat ~/Library/Application\ Support/Claude/config.json | grep -A 10 "openl-mcp-server"
```

Should be:
```json
"openl-mcp-server": {
  "command": "node",
  "args": ["/Users/asamuseu/IdeaProjects/openl-tablets/mcp-server/dist/index.js"],
  ...
}
```

### Step 4: Restart Claude Desktop

1. Completely close Claude Desktop (don't just minimize)
2. Start it again
3. Open settings → MCP Servers
4. Ensure `openl-mcp-server` is connected

### Step 5: Test in Claude

In Claude chat, write:

```
List repositories in OpenL Tablets
```

Claude should use MCP tools and show the list of repositories.

---

## 🎯 Method 2: Local Run (without Docker)

If you don't have Docker or want to run locally.

### Step 1: Build OpenL Tablets

```bash
cd /Users/asamuseu/IdeaProjects/openl-tablets
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

### Steps 4-5: Same as Method 1

Follow steps 3-5 from Method 1.

---

## 🔍 Health Check

### Automatic Check

```bash
cd /Users/asamuseu/IdeaProjects/openl-tablets/mcp-server
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
   ```bash
   cat ~/Library/Application\ Support/Claude/config.json | grep openl-mcp-server
   ```

3. **Does Claude Desktop see the server?**
   - Open Claude Desktop settings
   - Find MCP Servers section
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

### Issue: MCP Server doesn't appear in Claude Desktop

**Solution:**
1. Check path in configuration (must be absolute):
   ```bash
   cat ~/Library/Application\ Support/Claude/config.json
   ```
2. Ensure project is built:
   ```bash
   cd mcp-server
   npm run build
   ls -la dist/index.js  # Should exist
   ```
3. Completely restart Claude Desktop

### Issue: "Authentication failed" in Claude

**Solution:**
1. Check credentials in Claude Desktop configuration
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

---

## 📊 Startup Order (Brief Version)

```bash
# Terminal 1: Start OpenL Tablets
cd /Users/asamuseu/IdeaProjects/openl-tablets
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
# 3. Restart Claude Desktop
# 4. Check MCP server in Claude settings
# 5. Try in Claude: "List repositories in OpenL Tablets"
```

---

## ✅ Readiness Checklist

- [ ] OpenL Tablets is running and accessible at http://localhost:8080
- [ ] Can log into OpenL Studio via browser (admin/admin)
- [ ] MCP server is configured in Claude Desktop configuration
- [ ] Claude Desktop restarted after configuration
- [ ] MCP server is visible in Claude Desktop settings
- [ ] Claude can execute command "List repositories"

---

## 📚 Additional Information

- [README.md](./README.md) - Complete MCP server documentation
- [README-TESTING.md](./README-TESTING.md) - Detailed testing guide
- [AUTHENTICATION.md](./AUTHENTICATION.md) - Authentication setup

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
