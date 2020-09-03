package org.openl.rules.runtime;

/**
 * The class what represents information about rule.
 */
public class RuleInfo {

    static final RuleInfo[] EMPTY_RULES = new RuleInfo[0];
    /**
     * Rule name.
     */
    private String name;

    /**
     * Return type of rule.
     */
    private Class<?> returnType;

    /**
     * Formal parameters types.
     */
    private Class<?>[] paramTypes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }

    public Class<?>[] getParamTypes() {
        return paramTypes;
    }

    public void setParamTypes(Class<?>[] paramTypes) {
        this.paramTypes = paramTypes;
    }

}
