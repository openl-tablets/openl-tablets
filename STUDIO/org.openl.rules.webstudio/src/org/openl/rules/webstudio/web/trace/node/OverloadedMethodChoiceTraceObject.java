package org.openl.rules.webstudio.web.trace.node;

import java.util.List;

import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenMethod;

/**
 * Trace object for step of choosing the method from overloaded by properties
 * group of tables.
 *
 * @author PUdalau
 */
public class OverloadedMethodChoiceTraceObject extends ATableTracerNode {
    private List<IOpenMethod> methodCandidates;

    private OverloadedMethodChoiceTraceObject(ExecutableRulesMethod dispatcherTable,
            Object[] params,
            List<IOpenMethod> methodCandidates) {
        super("overloadedMethodChoice", null, dispatcherTable, params);
        this.methodCandidates = methodCandidates;
    }

    public List<IOpenMethod> getMethodCandidates() {
        return methodCandidates;
    }

    public static ATableTracerNode create(OpenMethodDispatcher dispatcher, Object[] params) {
        ExecutableRulesMethod method = (ExecutableRulesMethod) dispatcher.getDispatcherTable().getMember();
        return new OverloadedMethodChoiceTraceObject(method, params, dispatcher.getCandidates());
    }
}
