---
title: Deploy Project
description: OpenL deployment workflow with mandatory validation checks, test execution requirements, and environment selection (dev, test, staging, prod)
arguments:
  - name: projectId
    description: ID of project to deploy
    required: false
  - name: environment
    description: "Target environment: 'dev', 'test', 'staging', or 'prod'"
    required: false
---

## Summary

**Critical Pre-Deployment Checklist**: All deployments MUST pass validation (0 errors), run all tests (100% pass), and follow environment progression (dev → test → staging → prod). Use `openl_project_status` to validate and `openl_start_project_tests` + `openl_get_test_results` to run tests.

# OpenL Deployment Workflow

{if projectId}
## Deploying Project: **{projectId}**
{end if}
{if environment}

**Target Environment**: {environment}

### Environment-Specific Checks for {environment}:
{end if}

BEFORE any deployment (MANDATORY):
1. Validate project → MUST pass (0 errors)
   Use `openl_project_status(projectId)` to check `compileState` and review `diagnostics` (errors/warnings with location)
2. Run all tests → ALL must pass
   Use `openl_start_project_tests()` then `openl_get_test_results()` to run tests, or use OpenL Studio UI. The result tool waits internally until Studio has completed the test run.
3. Check for errors → MUST be 0
   Use `openl_project_status(projectId)` and confirm there are no error diagnostics

WHEN deploying, SELECT environment path:
- New feature/major change → dev → test → staging → prod{if environment} (You're targeting: {environment}){end if}
- Bug fix → test → staging → prod
- Minor update → test → prod
- Critical hotfix → test → prod (expedited)

IF deployment fails:
1. Use `openl_project_status(projectId)` to review validation issues (error diagnostics with location)
2. Fix errors and re-validate (saving with `openl_save_project()` also re-validates)
3. Redeploy

IF need rollback (manual process):
1. Use `openl_repository_project_revisions(projectId)` to retrieve committed revisions and identify a stable revision from before the problematic deployment
2. Restore the project to that stable revision in OpenL Studio UI (no MCP tool reverts a project to a previous revision)
3. Redeploy the restored version to the environment using `openl_deploy_project()` or `openl_redeploy_project()`

**When to use manual rollback:**
- Deployment fails validation or testing in production
- Critical bugs discovered after deployment
- Need to restore to a known-good state quickly
- Automatic deployment validation fails (requires manual intervention)

## OpenL Deployment Features

- **Atomic deployment**: All or nothing (entire OpenL project deployed)
- **Manual rollback**: Use `openl_repository_project_revisions()` to find a stable revision, restore it in OpenL Studio UI, then redeploy
- **Version history preserved**: All committed revisions are listed via `openl_repository_project_revisions()`
- **Audit trail**: Full deployment history in project commits
