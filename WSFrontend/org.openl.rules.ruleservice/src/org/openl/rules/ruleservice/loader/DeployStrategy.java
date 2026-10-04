package org.openl.rules.ruleservice.loader;

public enum DeployStrategy {

    /**
     * Always deploys project to the production repository
     */
    ALWAYS,
    /**
     * Deploys is disabled
     */
    NEVER,
    /**
     * Deploys project to the production repository only if it is absent there
     */
    IF_ABSENT

}
