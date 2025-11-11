# Deploying Projects to Environments

Guide AI assistants through safe deployment process with environment selection and validation.

## Quick Decision Tree

**Choose deployment environment based on change type:**

1. **New feature or major change** → dev → test → staging → prod
2. **Bug fix (non-critical)** → test → staging → prod
3. **Minor update** → test → prod
4. **Critical hotfix** → test → prod (expedited approval)

## Pre-Deployment Checklist

**Always run before deployment:**

```
1. validate_project → Must return success (0 errors)
2. run_all_tests → All tests must pass
3. get_project_errors → Must return 0 errors
```

If any step fails, fix issues before deploying.

## Environment Selection Guide

### Development (dev)
- **Purpose**: Active development and initial testing
- **When**: Every code change, feature development
- **Approval**: None required
- **Risk**: Low (isolated, easily reverted)
- **Deploy frequency**: Multiple times per day
- **Use for**: Rapid iteration, debugging, feature development

### Testing (test/qa)
- **Purpose**: QA validation and integration testing
- **When**: After dev testing passes, ready for QA review
- **Approval**: Optional (QA lead)
- **Risk**: Low (isolated environment)
- **Deploy frequency**: Daily or as needed
- **Use for**: Regression testing, QA validation, integration tests

### Staging (staging/uat)
- **Purpose**: Pre-production validation, business acceptance
- **When**: After test passes, ready for business review
- **Approval**: Required (product owner/business)
- **Risk**: Medium (mirrors production)
- **Deploy frequency**: Weekly or per release
- **Use for**: Business acceptance testing, final validation, demo

### Production (prod)
- **Purpose**: Live business operations
- **When**: After staging approval, scheduled release window
- **Approval**: Required (release manager, compliance)
- **Risk**: High (impacts live users and business)
- **Deploy frequency**: Scheduled releases only
- **Use for**: Live deployment to end users
- **Safety**: Atomic deployment with automatic rollback on failure

## Deployment Workflow

### Standard Deployment Path

```
1. Validate project
   validate_project(projectId) → Must pass

2. Run all tests
   run_all_tests(projectId) → All must pass

3. Deploy to first environment
   deploy_project(projectId, deploymentRepository: "dev")

4. Verify deployment
   list_deployments() → Confirm deployment success

5. Promote through environments
   dev → test → staging → prod (based on approvals)
```

### Fast Track (Minor Changes)

```
1. validate_project + run_all_tests
2. Deploy to test
3. If passing, deploy to prod (with approval)
```

### Hotfix Path (Critical Issues)

```
1. validate_project + run_all_tests (mandatory)
2. Deploy to test (quick smoke test)
3. Deploy to prod (expedited approval required)
4. Monitor closely post-deployment
```

## Common Deployment Patterns

### Pattern 1: Progressive Deployment (Recommended for Major Changes)
```
deploy_project(projectId, "dev") → verify →
deploy_project(projectId, "test") → QA validation →
deploy_project(projectId, "staging") → business approval →
deploy_project(projectId, "prod") → monitor
```

### Pattern 2: Fast Track (Minor Changes)
```
validate + test →
deploy_project(projectId, "test") → verify →
deploy_project(projectId, "prod") → monitor
```

### Pattern 3: Rollback (If Issues Found)
```
revert_version(projectId, previousCommitHash) →
validate + test →
deploy_project(projectId, environment)
```

## Deployment Safety

**Built-in Safety Features:**
- Atomic deployment (all or nothing)
- Automatic rollback on failure
- Version history preserved
- Previous version available for instant rollback
- Full audit trail maintained

**Manual Safety Checks:**
- Always validate before deploying
- Always run tests before deploying
- Deploy to lower environments first
- Verify deployment success before promoting
- Monitor logs after deployment

## Troubleshooting Deployments

### Deployment Fails
1. Check validation errors: `get_project_errors(projectId)`
2. Fix errors and re-validate
3. Run tests again
4. Retry deployment

### Tests Fail After Deployment
1. Check deployment logs
2. Compare versions: `compare_versions(projectId, oldCommit, newCommit)`
3. Revert if needed: `revert_version(projectId, oldCommit)`
4. Fix issues and redeploy

### Need to Rollback
1. Get project history: `get_project_history(projectId)`
2. Identify stable version
3. Revert: `revert_version(projectId, stableCommit)`
4. Redeploy: `deploy_project(projectId, environment)`

## Key Reminders

- **Never skip validation** - Even for "small" changes
- **Always run tests** - Before any deployment
- **Progress through environments** - Don't skip stages for major changes
- **Get approvals** - For staging and production deployments
- **Monitor after deployment** - Watch for errors and issues
- **Have rollback plan** - Know the last stable version
