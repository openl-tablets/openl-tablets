# Deploy Project - Prompt Template

## Purpose
Guide AI assistant through safe deployment process with environment selection and validation.

## Prompt

## Deployment Readiness Check

**Project:** `{project_name}`
**Current repository:** `{source_repository}`
**Branch:** `{branch_name}`

---

### Pre-Deployment Validation

{validation_in_progress_indicator}

✅ **Validation Results:**
- Syntax errors: {error_count} {error_status_icon}
- Warnings: {warning_count} {warning_status_icon}
- Test results: {test_pass_count}/{test_total_count} passed {test_status_icon}
- Last modified: {last_modified_date} by {last_modified_by}

{if errors_or_failures:}
❌ **Deployment Blocked**

**Errors found:**
{list_errors}

**Failed tests:**
{list_failed_tests}

**Action required:** Fix errors and failing tests before deployment.

Would you like me to help fix these issues? (yes/no)

{stop_here_if_blocked}
{end if}

---

### Available Deployment Environments

{for each environment:}
#### {index}. {environment_name} {recommended_badge}

**Repository:** `{deployment_repository_name}`
**Purpose:** {environment_description}
**Current version:** {current_deployed_version}
**Your access:** {permission_level} {permission_icon}

**Deployment frequency:** {frequency}
**Requires approval:** {approval_required} {approval_icon}
**Rollback capability:** {rollback_available} {rollback_icon}

{end for}

---

### Environment Details

#### Development (dev)
- **Purpose:** Development testing and integration
- **Audience:** Developers only
- **Data:** Test/synthetic data
- **Approval:** None required
- **Frequency:** Multiple times per day
- **Risk:** Low - can be quickly reverted

#### Testing (test/qa)
- **Purpose:** QA validation and user acceptance testing
- **Audience:** QA team, business analysts
- **Data:** Realistic test data
- **Approval:** QA lead (optional)
- **Frequency:** Daily to weekly
- **Risk:** Low - isolated environment

#### Staging (staging/uat)
- **Purpose:** Pre-production validation with production-like setup
- **Audience:** Business users, stakeholders
- **Data:** Production copy (anonymized)
- **Approval:** Product owner/business approval
- **Frequency:** Weekly or per release
- **Risk:** Medium - should mirror production

#### Production (prod/production)
- **Purpose:** Live business operations
- **Audience:** End users, customers
- **Data:** Real production data
- **Approval:** **Required** - Release manager, compliance
- **Frequency:** Scheduled releases only
- **Risk:** **High** - impacts live business

---

### Your Permissions

**User:** `{username}`
**Roles:** {user_roles}

**Deployment permissions:**
- ✅ dev: Allowed
- ✅ test: Allowed
- ✅ staging: Allowed {conditional}
- {permission_icon} production: {production_permission_status}

{if no_production_permission:}
⚠️ **Note:** Production deployment requires approval from release manager.
I can prepare the deployment, and you can request approval.
{end if}

---

### Deployment Impact Analysis

**This deployment will:**

**Rules changes:**
- Modified rules: {modified_rule_count}
- New rules: {new_rule_count}
- Removed rules: {removed_rule_count}

**Affected business processes:**
{list_affected_processes}

**Estimated impact:**
- Transactions affected: ~{estimated_transaction_count}/day
- Users affected: ~{estimated_user_count}
- Risk level: {risk_level} {risk_icon}

---

### Deployment Checklist

Before deploying to **{selected_environment}**, verify:

- [x] All tests passed ({test_pass_count}/{test_total_count})
- [x] No validation errors (0 errors)
- [ ] Code review completed {if production}
- [ ] Change request approved {if production}
- [ ] Stakeholder notification sent {if production/staging}
- [ ] Rollback plan documented {if production}
- [ ] Deployment window scheduled {if production}

{missing_checklist_items}

---

### Deployment Options

**Select environment:**

1. **dev** - Deploy to development ⚡ Immediate
2. **test** - Deploy to QA/testing 🧪 Immediate
3. **staging** - Deploy to staging 🎯 Requires business approval
4. **production** - Deploy to production 🚀 Requires full approval process

**Your choice:** (1-4)

---

### Recommended Path

{if first_deployment:}
**Recommendation:** Progressive deployment

1. Start with **dev** → Verify basic functionality
2. Then **test** → QA validation
3. Then **staging** → Business acceptance
4. Finally **production** → Go live

This reduces risk by validating at each stage.
{end if}

**For your changes ({change_type}):**
**Recommended:** Deploy to **{recommended_environment}** first

**Reason:** {recommendation_reason}

---

### Deployment Command

**Environment:** `{selected_environment}`
**Action:** Deploy project `{project_name}` from `{source_repository}` to `{deployment_repository}`

**Review details:**
- Source: {source_repository}/{project_name} ({branch})
- Target: {deployment_repository}
- Rules: {rule_count} tables
- Version: {project_version}

**Confirm deployment?** (yes/no)

---

### After Deployment

I will:
1. Execute deployment
2. Verify deployment success
3. Run smoke tests on deployed rules
4. Provide deployment summary
5. Monitor for immediate errors

If deployment fails:
- Automatic rollback to previous version
- Error analysis and root cause
- Suggested fixes

---

### Safety Notes

🛡️ **Production Deployment Safety:**

- Deployments to production create a new version (history preserved)
- Previous version remains available for instant rollback
- Deployment is atomic (all or nothing)
- Failed deployments auto-rollback
- Audit trail is maintained

**Ready to proceed?** (yes/no)
