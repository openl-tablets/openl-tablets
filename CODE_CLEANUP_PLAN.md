# Code Cleanup Plan - Phase 4

This document outlines all code cleanup tasks to remove trial-and-error artifacts and implement proper solutions.

---

## 1. Remove Misleading Comments

### Files to Clean

#### client.ts
❌ **Remove all "OpenL 6.0.0+ changed" comments**

These comments are meaningless since MCP only exists in 6.0.0:

```typescript
// Lines 110-111: Remove
- * 2. Base64 format: "ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n" (from OpenL 6.0.0+ API)

// Lines 150-152: Remove
- * OpenL 6.0.0+ uses base64-encoded IDs in URL paths.

// Lines 175-178: Remove
- * Build URL-safe project path for OpenL 6.0.0+ API
- *
- * OpenL 6.0.0+ changed the API structure to use base64-encoded project IDs:
- * - Old: /repos/{repository}/projects/{projectName}
- * - New: /projects/{base64-id}

// Lines 205-208: Remove
- * Get comprehensive project information including details, modules, and dependencies
- *
- * OpenL 6.0.0+ changed API - no longer has /info endpoint, all data in main response

// Lines 213-215: Remove
- // OpenL 6.0.0+ uses list endpoint with filtering
- // GET /projects/{id} returns 404, so we use GET /projects and filter

// Lines 233-235: Remove
- * OpenL 6.0.0+ changed project lifecycle - projects may be auto-opened
- * This method checks if project exists and is accessible

// Lines 241-242: Remove
- // In OpenL 6.0.0+, /open endpoint returns 404
- // Instead, verify project exists and is accessible by calling get_project

// Lines 254-256: Remove
- * OpenL 6.0.0+ changed project lifecycle - close endpoint may not exist
- * This method is now a no-op for compatibility

// Lines 261-262: Remove
- // In OpenL 6.0.0+, /close endpoint may return 404
- // Just verify project exists
```

#### utils.ts
❌ **Remove version-specific comments**

```typescript
// Lines 130-133: Simplify
- * OpenL Tablets API 6.0.0+ returns project IDs as base64-encoded strings in the format:
- * "repository:projectName" (e.g., "design:Example 1 - Bank Rating")
- *
- * Older versions may return project IDs as objects with {repository, projectName} structure.
+ * Parse project ID from OpenL API response.
+ * Handles both base64-encoded strings and object formats.

// Lines 145-146: Remove
- // Handle object format (older API versions or test mocks)
+ // Handle object format (from test mocks)

// Lines 153: Remove
- // Handle string format (OpenL Tablets 6.0.0+)
+ // Handle string format (base64-encoded)
```

---

## 2. Fix Broken Implementations

### Priority 1: get_project

**Current Implementation** (Lines 213-230):
```typescript
async getProject(projectId: string): Promise<Types.ComprehensiveProject> {
  const [repository, projectName] = this.parseProjectId(projectId);
  const allProjects = await this.listProjects({ repository });
  const project = allProjects.find(p => {
    const parsed = parseProjectIdUtil(p.id);
    return parsed.repository === repository && parsed.projectName === projectName;
  });
  if (!project) {
    throw new Error(`Project not found: ${projectName} in repository ${repository}`);
  }
  return project as any;
}
```

**Problem**:
- Inefficient (fetches all projects)
- May not return complete details
- Uses filtering workaround

**Solution** (after testing):
1. **If `GET /projects/{base64-id}` works**: Use direct endpoint
2. **If endpoint doesn't exist**: Document as limitation, keep workaround but optimize
3. **If response differs**: Adjust type definitions

**Action**: Wait for integration test results before fixing

---

### Priority 2: open_project

**Current Implementation** (Lines 241-250):
```typescript
async openProject(projectId: string): Promise<boolean> {
  try {
    await this.getProject(projectId);
    return true;
  } catch (error) {
    throw new Error(`Cannot open project: ${sanitizeError(error)}`);
  }
}
```

**Problem**:
- Doesn't actually open project
- Just verifies it exists
- No-op workaround

**Solution** (after testing):
1. **If `POST /projects/{id}/open` works**: Use proper endpoint
2. **If projects are always open**: Remove tool or make explicit no-op with documentation
3. **If different lifecycle**: Implement correct approach

**Action**: Wait for integration test results

---

### Priority 3: close_project

**Current Implementation** (Lines 261-270):
Similar issues as open_project

**Solution**: Same as open_project

---

## 3. Standardize Approach

### URL Encoding

**Current State**:
- Inconsistent use of `encodeURIComponent`
- Base64 IDs are URL-safe, shouldn't need encoding

**Standardization**:
1. **Repository names**: Always encode (may have spaces)
2. **Project names**: Always encode (may have spaces)
3. **Base64 IDs**: Don't encode (URL-safe by design)
4. **Table IDs**: Don't encode (hash strings, URL-safe)
5. **File names**: Always encode (may have spaces/special chars)

**Changes needed**:
```typescript
// Line 186: Remove unnecessary encoding
- return `/projects/${encodeURIComponent(base64Id)}`;
+ return `/projects/${base64Id}`;

// Verify all other usages
```

---

### Error Handling

**Current State**:
- Inconsistent error messages
- Some use `sanitizeError`, others don't
- Unclear what failed

**Standardization**:
1. **Always use `sanitizeError` for external errors**
2. **Include endpoint in error message**
3. **Consistent error format**: `"Failed to {action}: {details}"`

**Template**:
```typescript
try {
  // API call
} catch (error) {
  throw new Error(`Failed to {action}: ${sanitizeError(error)}`);
}
```

---

### Response Transformation

**Current State**:
- Some methods cast with `as any`
- Inconsistent type handling

**Standardization**:
1. **Avoid `as any`** - use proper type assertions
2. **Add response validation** where critical
3. **Document type differences** if API doesn't match types

---

## 4. Code Quality Improvements

### Remove Dead Code

**buildProjectPath usage**:
- Used by most methods ✓
- Consistent implementation ✓

**parseProjectId usage**:
- Used correctly ✓
- Handles all formats ✓

**toBase64ProjectId usage**:
- Used by buildProjectPath ✓
- Could be optimized but acceptable ✓

### Add Missing Documentation

**Methods needing better docs**:
1. `parseProjectId` - Document all 3 formats clearly
2. `toBase64ProjectId` - Explain when/why to use
3. `buildProjectPath` - Document URL structure

### Improve Type Safety

**Areas to improve**:
1. Remove `as any` casts
2. Add proper type guards
3. Validate responses match expected types

---

## 5. Implementation Order

### Step 1: Remove Misleading Comments
- [ ] Clean client.ts comments
- [ ] Clean utils.ts comments
- [ ] Remove version-specific language
- [ ] Focus on what works, not what changed

### Step 2: Wait for Integration Test Results
- [ ] Run integration tests against real instance
- [ ] Document which endpoints work/fail
- [ ] Update API_ENDPOINT_MAPPING.md
- [ ] Update API_RESPONSES.md

### Step 3: Fix Implementations Based on Results
- [ ] Fix get_project if better endpoint exists
- [ ] Fix open_project based on actual lifecycle
- [ ] Fix close_project based on actual lifecycle
- [ ] Remove workarounds if proper endpoints exist

### Step 4: Standardize
- [ ] Consistent URL encoding
- [ ] Consistent error handling
- [ ] Consistent type handling
- [ ] Remove `as any` casts

### Step 5: Final Cleanup
- [ ] Run linter
- [ ] Run all tests (unit + integration)
- [ ] Update documentation
- [ ] Commit with clear message

---

## Expected Outcome

After cleanup:
- ✅ No misleading "version changed" comments
- ✅ Implementations match actual API
- ✅ Consistent code style
- ✅ Proper error messages
- ✅ Type-safe code
- ✅ Clear documentation
- ✅ All tests passing

---

## Files to Modify

1. `mcp-server/src/client.ts` - Main cleanup
2. `mcp-server/src/utils.ts` - Comment cleanup
3. `mcp-server/src/types.ts` - Type adjustments if needed
4. `API_ENDPOINT_MAPPING.md` - Update status
5. `API_RESPONSES.md` - Fill in actual responses

---

## Next Action

**WAIT** for user to:
1. Run `./test-api-endpoints.sh`
2. Share `api-test-results.txt`
3. Fill in `API_RESPONSES.md` with actual responses

Then proceed with informed fixes rather than more guesswork.
