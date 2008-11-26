package org.openl.rules.ruleservice;

public interface RulesFrontend {

    public Object execute(String deployment, String ruleModule, String ruleName, Object... params);
    public Object execute(String deployment, String ruleModule, String ruleName, Class<?>[] inputParamsTypes, Object[] params);
    public Object getValues(String deployment, String ruleModule, String fieldName);

}
