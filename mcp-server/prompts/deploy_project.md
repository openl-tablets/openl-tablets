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
   Use `openl_start_project_tests()` + `openl_get_project_test_results()` to run tests, or use OpenL WebStudio UI
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

IF need rollback:
1. `openl_get_project_history()` → find stable Git commitHash
2. `openl_revert_version(targetVersion=commitHash)` → Creates new commit with old state
3. Redeploy to environment

## OpenL Deployment Features

- **Atomic deployment**: All or nothing (entire OpenL project deployed)
- **Automatic rollback**: On failure, OpenL reverts automatically
- **Version history preserved**: All Git commitHashes maintained
- **Instant rollback**: Previous version available via openl_revert_version()
- **Audit trail**: Full deployment history in project commits
