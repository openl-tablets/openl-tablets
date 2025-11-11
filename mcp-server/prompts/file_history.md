# Working with File History in OpenL Tablets

OpenL Tablets uses **Git for version control**. Every save/upload creates a Git commit automatically.

## Version Identifier = Git Commit Hash

Versions are identified by Git commit hashes (e.g., "7a3f2b1c3e..."), NOT sequential numbers like v1, v2, v3.

## Viewing File History

The `get_file_history` tool shows all commits that modified a specific file:

```
File: rules/Insurance.xlsx

1. Commit 7a3f2b1c
   Author: John Doe <john@company.com>
   Date: 2025-01-15T10:30:00Z
   Type: SAVE
   Comment: Updated premium calculation rules

2. Commit 9e5d8a2f
   Author: Jane Smith <jane@company.com>
   Date: 2025-01-10T14:20:00Z
   Type: SAVE
   Comment: Added state-specific rules for CA
```

## Commit Types

- **SAVE**: Normal save operation
- **ARCHIVE**: File archived (soft delete)
- **RESTORE**: File restored from archive
- **ERASE**: File permanently deleted
- **MERGE**: Git branch merge

## Using Commit Hashes

Once you have the commit hash, you can:

1. **Download specific version**: `download_file` with `version` parameter
   ```
   download_file(projectId, fileName, version="7a3f2b1c")
   ```

2. **Compare versions**: `compare_versions` between two commit hashes
   ```
   compare_versions(projectId, baseCommitHash="9e5d8a2f", targetCommitHash="7a3f2b1c")
   ```

3. **Revert to previous version**: `revert_version` to restore old state
   ```
   revert_version(projectId, targetVersion="9e5d8a2f", comment="Revert to stable version")
   ```

## Pagination

For files with many commits, use `limit` and `offset`:
- `limit: 50` - Get 50 commits at a time (default)
- `offset: 0` - Start from beginning (default)
- `offset: 50` - Skip first 50, get next 50

Example:
```
# Get first page
get_file_history(projectId, filePath, limit=50, offset=0)

# Get second page
get_file_history(projectId, filePath, limit=50, offset=50)
```

## Important Notes

- **File names never change** between versions
- Same file path exists across all commits with different content
- Each commit is a snapshot of the entire project state
- Reverting creates a NEW commit (doesn't delete history)
- Git branching is supported (use branches for parallel development)

## Typical Workflow

1. **List file history** to see all versions:
   ```
   get_file_history(projectId="design_Insurance", filePath="rules/Premium.xlsx")
   ```

2. **Download a specific version** to inspect:
   ```
   download_file(projectId="design_Insurance", fileName="rules/Premium.xlsx", version="7a3f2b1c")
   ```

3. **Compare two versions** to see what changed:
   ```
   compare_versions(projectId="design_Insurance", baseCommitHash="9e5d8a2f", targetCommitHash="7a3f2b1c")
   ```

4. **Revert if needed** to restore a previous version:
   ```
   revert_version(projectId="design_Insurance", targetVersion="9e5d8a2f", comment="Revert to stable version before bug was introduced")
   ```

## File Path Format

File paths are relative to the project root:
- ✅ `rules/Insurance.xlsx`
- ✅ `rules/subdir/Premium.xlsx`
- ✅ `data/TestData.xlsx`
- ❌ `/design/design_Insurance/rules/Insurance.xlsx` (absolute path - incorrect)

## Error Handling

If a file doesn't exist or was deleted:
- History shows all commits including deletion
- Download of deleted file version from before deletion still works
- ERASE commit type indicates permanent deletion
