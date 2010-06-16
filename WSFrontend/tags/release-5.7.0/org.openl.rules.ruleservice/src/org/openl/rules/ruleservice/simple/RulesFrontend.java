package org.openl.rules.ruleservice.simple;

public interface RulesFrontend {

    /**
     * Executes method with specified parameters.
     *
     * @param deployment Name of deployment in production repository
     * @param ruleModule Name of rules module in the deployment
     * @param ruleName Technical name of the rule to execute
     * @param inputParamsTypes Types of method input parameters to discover
     *            method
     * @param params Parameters for method execution
     * @return Result of execution
     */
    Object execute(String deployment, String ruleModule, String ruleName, Class<?>[] inputParamsTypes,
            Object[] params);

    /**
     * Executes method with specified parameters. Method discovery is done based
     * on parameters types.
     *
     * @param deployment Name of deployment in production repository
     * @param ruleModule Name of rules module in the deployment
     * @param ruleName Technical name of the rule to execute
     * @param params Parameters for method execution
     * @return Result of execution
     */
    Object execute(String deployment, String ruleModule, String ruleName, Object... params);

    /**
     * Gets values defined in rules. 
     *
     * @param deployment Name of deployment in production repository
     * @param ruleModule Name of rules module in the deployment
     * @param fieldName Technical name of the rule to execute
     * @return Data stored in field
     */
    Object getValues(String deployment, String ruleModule, String fieldName);

}
