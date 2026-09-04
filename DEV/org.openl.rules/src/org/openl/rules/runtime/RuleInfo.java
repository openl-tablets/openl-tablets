package org.openl.rules.runtime;

import java.util.Arrays;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

/**
 * The class what represents information about rule.
 */
public class RuleInfo {

    /**
     * Rule name.
     */
    @Getter
    @Setter
    private String name;

    /**
     * Return type of rule.
     */
    @Getter
    @Setter
    private Class<?> returnType;

    /**
     * Formal parameters types.
     */
    @Getter
    @Setter
    private Class<?>[] paramTypes;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        var ruleInfo = (RuleInfo) o;
        return Objects.equals(name, ruleInfo.name) && Arrays.equals(paramTypes, ruleInfo.paramTypes);
    }

    @Override
    public int hashCode() {
        var result = Objects.hash(name);
        result = 31 * result + Arrays.hashCode(paramTypes);
        return result;
    }
}
