package org.openl.rules.webstudio.web.trace.node;

import java.util.List;

import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.runtime.IRuntimeContext;
import org.openl.types.IOpenMethod;

/**
 * Trace object for step of choosing the method from overloaded by properties group of tables.
 *
 * @author PUdalau
 */
public class OverloadedMethodChoiceTraceObject extends ATableTracerNode {
    private final List<IOpenMethod> methodCandidates;

    OverloadedMethodChoiceTraceObject(ExecutableRulesMethod dispatcherTable,
            Object[] params,
            IRuntimeContext context,
            List<IOpenMethod> methodCandidates) {
        super("overloadedMethodChoice", null, dispatcherTable, params, context);
        this.methodCandidates = methodCandidates;
    }

    public List<IOpenMethod> getMethodCandidates() {
        return methodCandidates;
    }

    static ATableTracerNode create(OpenMethodDispatcher dispatcher, Object[] params, IRuntimeContext context) {
        ExecutableRulesMethod method = (ExecutableRulesMethod) dispatcher.getDispatcherTable().getMember();
        return new OverloadedMethodChoiceTraceObject(method, params, context, dispatcher.getCandidates());
    }
}
