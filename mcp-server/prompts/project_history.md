# Working with Project History in OpenL Tablets

OpenL Tablets uses **Git for version control** at the project level. Every change that saves the project creates a Git commit.

## Project-Wide History

The `get_project_history` tool shows all commits across the entire project, not just a single file:

```
Project: design_Insurance

1. Commit 7a3f2b1c
   Author: John Doe <john@company.com>
   Date: 2025-01-15T10:30:00Z
   Branch: main
   Type: SAVE
   Files Changed: 3
   Tables Changed: 5
   Comment: Updated premium rules for all states

2. Commit 9e5d8a2f
   Author: Jane Smith <jane@company.com>
   Date: 2025-01-10T14:20:00Z
   Branch: main
   Type: SAVE
   Files Changed: 1
   Tables Changed: 2
   Comment: Added CA-specific rules
```

## What It Shows

Each commit in project history includes:

- **Commit Hash**: Git commit identifier (e.g., "7a3f2b1c")
- **Author**: Who made the change (name and email)
- **Timestamp**: When the change was made
- **Branch**: Git branch where commit was made
- **Commit Type**: SAVE, MERGE, ARCHIVE, etc.
- **Files Changed**: Number of Excel files modified
- **Tables Changed**: Number of rule tables modified (if available)
- **Comment**: Description of the change

## Commit Types

- **SAVE**: Normal save operation (most common)
- **ARCHIVE**: Project or files archived
- **RESTORE**: Restored from archive
- **ERASE**: Permanent deletion
- **MERGE**: Git branch merge

## Branch Support

OpenL Tablets supports Git branching:

```
# Get history for specific branch
get_project_history(projectId="design_Insurance", branch="development")

# Get history for main branch (default)
get_project_history(projectId="design_Insurance")
```

Branches enable:
- Parallel development
- Feature isolation
- Safe experimentation
- Release management

## Pagination

For projects with extensive history:

```
# Get first 50 commits (default)
get_project_history(projectId="design_Insurance", limit=50, offset=0)

# Get next 50 commits
get_project_history(projectId="design_Insurance", limit=50, offset=50)

# Get fewer commits per page
get_project_history(projectId="design_Insurance", limit=20)
```

Parameters:
- `limit`: Max commits to return (default: 50, max: 200)
- `offset`: Number of commits to skip (default: 0)
- `hasMore`: Boolean indicating if more commits exist

## Use Cases

### 1. Audit Trail

Track all changes made to a project:
```
get_project_history(projectId="design_Insurance")
```

Shows who made what changes and when, providing complete audit trail for compliance.

### 2. Find Specific Change

Search through history to find when a change was introduced:
```
get_project_history(projectId="design_Insurance", limit=200)
# Review comments to find relevant commit
```

### 3. Compare Project States

Get commit hashes to compare project at different points in time:
```
# Get recent commits
history = get_project_history(projectId="design_Insurance", limit=10)

# Compare two commits
compare_versions(
  projectId="design_Insurance",
  baseCommitHash=history.commits[5].commitHash,
  targetCommitHash=history.commits[0].commitHash
)
```

### 4. Track Team Activity

Monitor team changes by reviewing author and timestamp:
```
# Get recent activity
history = get_project_history(projectId="design_Insurance", limit=50)
# Group by author or date range
```

### 5. Revert to Stable State

Find a known-good commit and revert:
```
# Find stable commit in history
history = get_project_history(projectId="design_Insurance")

# Revert to that commit
revert_version(
  projectId="design_Insurance",
  targetVersion=history.commits[10].commitHash,
  comment="Revert to stable version from last week"
)
```

## Difference from File History

| Feature | Project History | File History |
|---------|----------------|--------------|
| **Scope** | Entire project | Single file |
| **Commits** | All commits to project | Only commits that touched file |
| **Files Changed** | Count of files | N/A |
| **Tables Changed** | Count of tables | N/A |
| **Branch** | Supported | Implicit (from project) |
| **Use Case** | Audit, overview | File-specific tracking |

## Typical Workflow

1. **Review recent changes** to understand project evolution:
   ```
   get_project_history(projectId="design_Insurance", limit=20)
   ```

2. **Identify interesting commit** from the list:
   ```
   # Note commit hash from results, e.g., "7a3f2b1c"
   ```

3. **Compare with current state** to see what's different:
   ```
   compare_versions(
     projectId="design_Insurance",
     baseCommitHash="7a3f2b1c",
     targetCommitHash="HEAD"  # or latest commit hash
   )
   ```

4. **Download specific file version** if needed:
   ```
   download_file(
     projectId="design_Insurance",
     fileName="rules/Premium.xlsx",
     version="7a3f2b1c"
   )
   ```

5. **Revert if necessary** to restore previous state:
   ```
   revert_version(
     projectId="design_Insurance",
     targetVersion="7a3f2b1c",
     comment="Revert to version before regression"
   )
   ```

## Important Notes

- Project history is **linear** within a branch
- Merges from other branches appear as MERGE commits
- Each commit represents a **full project snapshot**
- History is **immutable** (cannot be deleted or edited)
- Reverting creates a **new commit** (preserves history)
- Large projects may have thousands of commits (use pagination)

## Performance Considerations

- First page (offset=0) is fastest
- Large offsets may be slower
- Use appropriate limit (50 is good balance)
- Branch filtering reduces result set
- Consider caching results for repeated queries

## Integration with Other Tools

Combine project history with other tools:

```
# Get history
history = get_project_history(projectId="design_Insurance")

# For each commit, could:
# - compare_versions() to see changes
# - download_file() with version to inspect files
# - View author/timestamp for reporting
# - Filter by commit type for specific operations
```
