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

# OpenL Deployment Workflow

{if projectId}
## Deploying Project: **{projectId}**
{end if}
{if environment}

**Target Environment**: {environment}

### Environment-Specific Checks for {environment}:
{end if}

BEFORE any deployment (MANDATORY):
1. `validate_project({if projectId}projectId="{projectId}"{end if})` → MUST pass (0 errors)
2. `run_test({if projectId}projectId="{projectId}", {end if}runAllTests: true)` → ALL must pass
3. `get_project_errors({if projectId}projectId="{projectId}"{end if})` → MUST be 0

WHEN deploying, SELECT environment path:
- New feature/major change → dev → test → staging → prod{if environment} (You're targeting: {environment}){end if}
- Bug fix → test → staging → prod
- Minor update → test → prod
- Critical hotfix → test → prod (expedited)

IF deployment fails:
1. `get_project_errors()` → identify OpenL validation issues
2. Fix errors and re-validate
3. Redeploy

IF need rollback:
1. `get_project_history()` → find stable Git commitHash
2. `revert_version(targetVersion=commitHash)` → Creates new commit with old state
3. Redeploy to environment

## OpenL Deployment Features

- **Atomic deployment**: All or nothing (entire OpenL project deployed)
- **Automatic rollback**: On failure, OpenL reverts automatically
- **Version history preserved**: All Git commitHashes maintained
- **Instant rollback**: Previous version available via revert_version()
- **Audit trail**: Full deployment history in project commits
