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

---

## Example Usage Scenarios

### Scenario 1: View Recent Changes

**User asks**: "Who changed the Insurance rules last week?"

**Your action**:
1. Use `get_file_history` with `filePath: "rules/Insurance.xlsx"`, `limit: 20`
2. Review commits from last 7 days
3. Report who made changes and what they did

**Example Request**:
```json
{
  "projectId": "design-InsuranceRules",
  "filePath": "rules/Insurance.xlsx",
  "limit": 20
}
```

**Example Response**:
```
File history for rules/Insurance.xlsx:

Total commits: 156
Showing: 20

1. Commit 7a3f2b1c
   Author: John Doe <john@company.com>
   Date: 2025-01-15T10:00:00Z
   Type: SAVE
   Comment: Updated CA premium rates for 2025

2. Commit e4f5g6h7
   Author: Jane Smith <jane@company.com>
   Date: 2025-01-12T14:30:00Z
   Type: SAVE
   Comment: Fixed calculation bug in deductible logic

3. Commit a1b2c3d4
   Author: John Doe <john@company.com>
   Date: 2025-01-08T09:15:00Z
   Type: SAVE
   Comment: Added support for multi-car discount
```

**Your response to user**: "The Insurance.xlsx file was changed 3 times last week:
1. John Doe updated CA premium rates on Jan 15
2. Jane Smith fixed a deductible calculation bug on Jan 12
3. John Doe added multi-car discount support on Jan 8"

---

### Scenario 2: Find Version from Specific Date

**User asks**: "I need the version of rules from December 15th to compare against current"

**Your action**:
1. Use `get_file_history` with larger limit to find commits from December
2. Identify commit closest to December 15th
3. Use that commit hash with `download_file` to retrieve it
4. Use `compare_versions` to show differences

**Workflow**:
```
Step 1: Get history
get_file_history(
  projectId="design-InsuranceRules",
  filePath="rules/Premium.xlsx",
  limit=100
)

Step 2: Find Dec 15 commit → "9e5d8a2f" (closest match)

Step 3: Download that version
download_file(
  projectId="design-InsuranceRules",
  fileName="rules/Premium.xlsx",
  version="9e5d8a2f"
)

Step 4: Compare with current
compare_versions(
  projectId="design-InsuranceRules",
  baseCommitHash="9e5d8a2f",  # Dec 15 version
  targetCommitHash="7a3f2b1c"  # Current version
)
```

---

### Scenario 3: Investigate When Bug Was Introduced

**User asks**: "The premium calculation seems wrong. Can you find when it changed?"

**Your action**:
1. Get file history for the rules file
2. Look for commits that mention "premium" or "calculation"
3. Compare versions before/after suspicious commits
4. Identify which commit introduced the issue

**Workflow**:
```
Step 1: Get history
get_file_history(projectId="design-InsuranceRules", filePath="rules/Premium.xlsx", limit=50)

Step 2: Review commit comments, find suspicious one:
- Commit a1b2c3d4: "Updated premium formula for high-risk drivers"

Step 3: Compare version before and after
compare_versions(
  projectId="design-InsuranceRules",
  baseCommitHash="previousCommit",  # Before the change
  targetCommitHash="a1b2c3d4"       # After the change
)

Step 4: If this is the bug, revert it
revert_version(
  projectId="design-InsuranceRules",
  targetVersion="previousCommit",
  comment="Revert buggy premium formula change"
)
```

---

### Scenario 4: Audit Trail for Compliance

**User asks**: "I need a complete audit trail of all changes to ClaimRules.xlsx in Q4 2024"

**Your action**:
1. Get full file history with pagination
2. Filter commits by date range (Oct 1 - Dec 31, 2024)
3. Format as audit report with author, date, and changes

**Workflow**:
```
# Get all commits in batches
page1 = get_file_history(projectId="design-ClaimProcessing", filePath="ClaimRules.xlsx", limit=50, offset=0)
page2 = get_file_history(projectId="design-ClaimProcessing", filePath="ClaimRules.xlsx", limit=50, offset=50)
page3 = get_file_history(projectId="design-ClaimProcessing", filePath="ClaimRules.xlsx", limit=50, offset=100)

# Filter by Q4 2024 dates
# Format audit report with commit hash, author, timestamp, comment
```

---

### Scenario 5: Recover Deleted File

**User asks**: "Someone deleted the TestScenarios.xlsx file. Can you recover it?"

**Your action**:
1. Get file history (will show deletion commit)
2. Find last commit BEFORE deletion (Type: SAVE)
3. Download that version
4. Upload it back to the project

**Workflow**:
```
Step 1: Get history (even for deleted files)
get_file_history(projectId="design-Testing", filePath="TestScenarios.xlsx", limit=20)

Response shows:
1. Commit xyz123: Type: ERASE, Date: 2025-01-10
2. Commit abc456: Type: SAVE, Date: 2025-01-09 ← Last good version

Step 2: Download last good version
download_file(
  projectId="design-Testing",
  fileName="TestScenarios.xlsx",
  version="abc456"
)

Step 3: Upload it back
upload_file(
  projectId="design-Testing",
  fileName="TestScenarios.xlsx",
  fileContent="<base64 from step 2>",
  comment="Recovered deleted TestScenarios file"
)
```

---

## Common Patterns

### Pattern 1: Before/After Comparison
```
get_file_history() → identify commit → compare_versions() → review changes
```

### Pattern 2: Time Travel
```
get_file_history() → find date → download_file(version) → inspect old version
```

### Pattern 3: Rollback
```
get_file_history() → identify good version → revert_version() → restore state
```

### Pattern 4: Investigation
```
get_file_history() → review commits → download_file() for each → find issue
```

### Pattern 5: Audit
```
get_file_history(large limit) → export all commits → generate report
```
