package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.runtime.IRuntimeContext;

public class ATableTracerNode extends SimpleTracerObject {

    private Object params[];
    private ExecutableRulesMethod method;
    private String prefix;
    private final IRuntimeContext context;

    ATableTracerNode(String type, String prefix, ExecutableRulesMethod method, Object[] params) {
        this(type, prefix, method, params, null);
    }

    ATableTracerNode(String type,
            String prefix,
            ExecutableRulesMethod method,
            Object[] params,
            IRuntimeContext context) {
        super(type);
        this.prefix = prefix;
        this.method = method;
        CachingArgumentsCloner cloner = CachingArgumentsCloner.getInstance();
        if (params != null) {
            Object[] clonedParams;
            try {
                clonedParams = cloner.deepClone(params);
            } catch (Throwable e) {
                // ignore cloning exception if any, use params itself
                //
                clonedParams = params;
            }
            this.params = clonedParams;
        } else {
            this.params = new Object[0];
        }

        this.context = context;
    }

    public ExecutableRulesMethod getTraceObject() {
        if (method != null) {
            return method;
        }
        ITracerObject parent = getParent();
        if (parent instanceof ATableTracerNode) {
            return ((ATableTracerNode) parent).getTraceObject();
        }
        throw new IllegalStateException("The executable method is not defined");
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public Object[] getParameters() {
        return params;
    }

    @Override
    public String getUri() {
        return getTraceObject().getSourceUrl();
    }

    public IRuntimeContext getContext() {
        return context;
    }
}
