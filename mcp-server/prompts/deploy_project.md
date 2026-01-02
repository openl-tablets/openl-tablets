---
name: deploy_project
description: OpenL deployment workflow with validation checks and environment selection
arguments:
  - name: projectId
    description: ID of project to deploy
    required: false
  - name: environment
    description: Target environment (dev, test, staging, prod)
    required: false
---

## Summary

**Critical Pre-Deployment Checklist**: All deployments MUST pass validation (0 errors), run all tests (100% pass), and follow environment progression (dev → test → staging → prod). Use OpenL WebStudio UI for validation and testing since MCP tools are temporarily disabled.

# OpenL Deployment Workflow

{if projectId}
## Deploying Project: **{projectId}**
{end if}
{if environment}

**Target Environment**: {environment}

### Environment-Specific Checks for {environment}:
{end if}

BEFORE any deployment (MANDATORY):
1. Validate project manually → MUST pass (0 errors)
   Note: `openl_validate_project` is temporarily disabled - use OpenL WebStudio UI to validate
2. Run all tests → ALL must pass
   Use `openl_run_project_tests()` to run tests (unified tool), or use OpenL WebStudio UI
3. Check for errors → MUST be 0
   Note: `openl_get_project_errors` is temporarily disabled - use OpenL WebStudio UI

WHEN deploying, SELECT environment path:
- New feature/major change → dev → test → staging → prod{if environment} (You're targeting: {environment}){end if}
- Bug fix → test → staging → prod
- Minor update → test → prod
- Critical hotfix → test → prod (expedited)

IF deployment fails:
1. Check OpenL WebStudio UI for validation issues
   (or use `openl_get_project_errors` when re-enabled)
2. Fix errors and re-validate
3. Redeploy

IF need rollback (manual process):
1. Use `openl_get_project_history(projectId)` to retrieve Git commit history and identify a stable commit hash from before the problematic deployment
2. Use `openl_revert_version(projectId, targetVersion=commitHash, confirm=true)` to create a new commit that restores the project state to the selected stable version
3. Redeploy the reverted version to the environment using `openl_deploy_project()` or `openl_redeploy_project()`

**When to use manual rollback:**
- Deployment fails validation or testing in production
- Critical bugs discovered after deployment
- Need to restore to a known-good state quickly
- Automatic deployment validation fails (requires manual intervention)

## OpenL Deployment Features

- **Atomic deployment**: All or nothing (entire OpenL project deployed)
- **Manual rollback**: Use `openl_get_project_history()` + `openl_revert_version()` + redeploy to restore previous version
- **Version history preserved**: All Git commitHashes maintained for easy rollback
- **Instant rollback**: Previous version available via `openl_revert_version()` - creates new commit with old state
- **Audit trail**: Full deployment history in project commits
