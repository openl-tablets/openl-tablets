# How to Enable Disabled MCP Tools

## Overview

Several MCP tools are currently disabled (commented out) in the codebase. They are marked with `TEMPORARILY DISABLED` comments and wrapped in multi-line comments `/* ... */`.

## Disabled Tools List

The following tools are currently disabled:

> **Note:** Line numbers are approximate and may change across versions. Always search for the tool name to locate the code.

1. **`openl_upload_file`**
   - Upload Excel files to a project
   - Reason: "Tool is not working correctly and needs implementation fixes"

2. **`openl_download_file`**
   - Download Excel files from a project
   - Reason: "Tool is not working correctly and needs implementation fixes"

3. ~~**`openl_create_rule`**~~ **REMOVED**
   - ~~Create new tables/rules in a project~~
   - **Status**: Permanently removed (returned 405 Method Not Allowed in OpenL 6.0.0)
   - **Replacement**: Use `openl_create_project_table` instead, which uses BETA API and works correctly

4. **`openl_execute_rule`**
   - Execute rules with input data for testing
   - Reason: "Tool is not working correctly and needs implementation fixes"

5. **`openl_revert_version`**
   - Revert project to a previous Git commit
   - Reason: "Tool is not working correctly and needs implementation fixes"

6. **`openl_get_file_history`**
   - Get Git commit history for a specific file
   - Reason: "Tool is not working correctly and needs implementation fixes"

7. **`openl_get_project_history`**
   - Get Git commit history for entire project
   - Reason: "Tool is not working correctly and needs implementation fixes"

## How to Enable a Tool

To enable any of these tools:

1. **Open the file:**
   ```bash
   mcp-server/src/tool-handlers.ts
   ```

2. **Find the disabled tool** (search for the tool name, e.g., search for `openl_upload_file`)

3. **Uncomment the code block:**
   - Remove the `/*` at the beginning
   - Remove the `*/` at the end
   - Optionally remove or update the `TEMPORARILY DISABLED` comment

4. **Rebuild the project:**
   ```bash
   cd mcp-server
   npm run build
   ```

5. **Restart the MCP server** (restart Claude Desktop or Cursor)

## Example: Enabling `openl_upload_file`

**Before (disabled):**
```typescript
  // TEMPORARILY DISABLED - openl_upload_file
  // Tool is not working correctly and needs implementation fixes
  /*
  registerTool({
    name: "openl_upload_file",
    // ... tool definition ...
  });
  */
```

**After (enabled):**
```typescript
  registerTool({
    name: "openl_upload_file",
    // ... tool definition ...
  });
```

## Important Notes

⚠️ **Warning:** These tools are disabled because they may not work correctly. Before enabling:

1. **Test thoroughly** - Make sure the tool works with your OpenL Tablets instance
2. **Check client.ts** - Some tools may require additional methods in `client.ts`
3. **Review error handling** - Ensure proper error handling is in place
4. **Test edge cases** - Test with various inputs and scenarios

## Checking if a Tool is Enabled

After enabling a tool, verify it's available:

1. **In Claude Desktop/Cursor:**
   - Check the tools list in MCP settings
   - The tool should appear in the available tools

2. **Via MCP protocol:**
   - The tool will be listed in the `tools/list` response
   - You can call it using `tools/call` with the tool name

## Troubleshooting

If a tool doesn't work after enabling:

1. **Check the logs** - Look for errors in MCP server logs
2. **Verify client methods** - Ensure `client.ts` has the required methods
3. **Check API compatibility** - Verify the OpenL Tablets API supports the operation
4. **Review schemas** - Make sure input schemas match the API requirements

## Related Files

- **Tool definitions:** `mcp-server/src/tool-handlers.ts`
- **Client methods:** `mcp-server/src/client.ts`
- **Schemas:** `mcp-server/src/schemas.ts`
- **Types:** `mcp-server/src/types.ts`





