# 🚀 How to Launch Claude Desktop

## Quick Launch

### Method 1: Via Finder
1. Open Finder
2. Go to "Applications" folder
3. Find "Claude"
4. Double-click to launch

### Method 2: Via Spotlight
1. Press `Cmd + Space` (open Spotlight)
2. Type "Claude"
3. Press Enter

### Method 3: Via Terminal
```bash
open -a Claude
```

### Method 4: Via Launchpad
1. Press F4 or use "pinch with thumb and three fingers" gesture
2. Find Claude icon
3. Click to launch

---

## First Launch

On first launch of Claude Desktop:

1. **Sign in to Anthropic Account**
   - If you don't have an account, create one at https://claude.ai
   - Sign in with your credentials

2. **Configure MCP Servers** (already done!)
   - Configuration is already copied to:
     `~/Library/Application Support/Claude/config.json`
   - After first launch, MCP servers will connect automatically

3. **Check MCP Connection**
   - Open settings (Settings)
   - Find "MCP Servers" or "Model Context Protocol" section
   - Ensure `openl-mcp-server` is displayed

---

## Verifying MCP Server Operation

After launching Claude Desktop:

### 1. Check Status in Settings

1. Open Claude Desktop
2. Click settings icon (⚙️) or `Cmd + ,`
3. Find "MCP Servers" or "Model Context Protocol" section
4. Should see `openl-mcp-server` with status:
   - ✅ "Connected" - everything works!
   - ⚠️ "Disconnected" - check configuration
   - ❌ Error - see logs

### 2. Test in Chat

In Claude chat, write:

```
List repositories in OpenL Tablets
```

If everything works, Claude will use MCP tools and show the list of repositories.

---

## If MCP Server Doesn't Connect

### Check 1: Configuration

```bash
cat ~/Library/Application\ Support/Claude/config.json
```

Ensure there's a section:
```json
{
  "mcpServers": {
    "openl-mcp-server": {
      "command": "node",
      "args": ["/Users/asamuseu/IdeaProjects/openl-tablets/mcp-server/dist/index.js"],
      ...
    }
  }
}
```

### Check 2: File Exists

```bash
ls -la /Users/asamuseu/IdeaProjects/openl-tablets/mcp-server/dist/index.js
```

If file doesn't exist, build the project:
```bash
cd /Users/asamuseu/IdeaProjects/openl-tablets/mcp-server
npm run build
```

### Check 3: Claude Desktop Logs

**macOS:**
```bash
# View latest logs
tail -f ~/Library/Logs/Claude/*.log

# Or via Console.app
open -a Console
# Then find Claude Desktop logs
```

Look for errors related to:
- `openl-mcp-server`
- `OPENL_BASE_URL`
- `node dist/index.js`

### Check 4: Restart

1. Completely close Claude Desktop (don't just minimize)
   - `Cmd + Q` or via menu "Claude" → "Quit Claude"
2. Wait a few seconds
3. Launch again
4. Check MCP server status

---

## Auto-launch Claude Desktop

If you want Claude Desktop to launch automatically:

1. Open "System Preferences"
2. Go to "Users & Groups"
3. Select "Login Items" tab
4. Click "+" and add Claude
5. Check "Hide" checkbox if needed

---

## Keyboard Shortcuts

- `Cmd + N` - New chat
- `Cmd + K` - Quick search
- `Cmd + ,` - Settings
- `Cmd + Q` - Quit
- `Cmd + W` - Close window

---

## Requirements

- **macOS:** 12.0 (Monterey) or newer
- **Internet:** Required for Claude AI
- **Node.js:** Already installed (v22.16.0) ✅

---

## Useful Links

- [Official Claude Desktop Website](https://claude.ai/download)
- [MCP Documentation](https://modelcontextprotocol.io/)
- [MCP Server Setup](https://claude.ai/docs/mcp)

---

## Quick Health Check

After launching Claude Desktop, run:

```bash
cd /Users/asamuseu/IdeaProjects/openl-tablets/mcp-server
./check-health.sh
```

This will check:
- ✅ OpenL Tablets is accessible
- ✅ MCP server is built
- ✅ Configuration is correct
- ✅ Everything is ready to work
