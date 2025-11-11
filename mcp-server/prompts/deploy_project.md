# OpenL Deployment Workflow

BEFORE any deployment (MANDATORY):
1. `validate_project()` → MUST pass (0 errors)
2. `run_test(runAllTests: true)` → ALL must pass
3. `get_project_errors()` → MUST be 0

WHEN deploying, SELECT environment path:
- New feature/major change → dev → test → staging → prod
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
