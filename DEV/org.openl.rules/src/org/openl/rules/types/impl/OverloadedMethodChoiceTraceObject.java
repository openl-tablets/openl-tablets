package org.openl.rules.types.impl;

import java.util.List;

import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ATableTracerNode;
import org.openl.types.IOpenMethod;

/**
 * Trace object for step of choosing the method from overloaded by properties
 * group of tables.
 *
 * @author PUdalau
 */
public class OverloadedMethodChoiceTraceObject extends ATableTracerNode {
    private List<IOpenMethod> methodCandidates;

    public OverloadedMethodChoiceTraceObject(ExecutableRulesMethod dispatcherTable,
            Object[] params,
            List<IOpenMethod> methodCandidates) {
        super("overloadedMethodChoice", null, dispatcherTable, params);
        this.methodCandidates = methodCandidates;
    }

    public List<IOpenMethod> getMethodCandidates() {
        return methodCandidates;
    }
}
