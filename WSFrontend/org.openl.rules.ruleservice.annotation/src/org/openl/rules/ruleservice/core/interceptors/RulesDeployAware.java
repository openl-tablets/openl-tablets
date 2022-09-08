package org.openl.rules.ruleservice.core.interceptors;

import org.openl.rules.project.model.RulesDeploy;

/**
 * This interface is designed to inject @{@link RulesDeploy} related to compiled project to ruleservice interceptors. If
 * a project doesn't have @{@link RulesDeploy} then null be injected.
 */
public interface RulesDeployAware {
    void setRulesDeploy(RulesDeploy rulesDeploy);
}
