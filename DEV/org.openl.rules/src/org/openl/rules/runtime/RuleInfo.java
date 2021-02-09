package org.openl.rules.runtime;

import java.util.Arrays;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RuleInfo ruleInfo = (RuleInfo) o;
        return Objects.equals(name, ruleInfo.name) && Arrays.equals(paramTypes, ruleInfo.paramTypes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name);
        result = 31 * result + Arrays.hashCode(paramTypes);
        return result;
    }
}
