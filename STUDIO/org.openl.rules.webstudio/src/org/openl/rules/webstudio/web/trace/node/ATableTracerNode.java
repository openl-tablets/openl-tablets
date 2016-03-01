package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.OpenLArgumentsCloner;

public class ATableTracerNode extends SimpleTracerObject {

    private Object params[];
    private ExecutableRulesMethod method;
    private String prefix;

    public ATableTracerNode(String type, String prefix, ExecutableRulesMethod method, Object[] params) {
        super(type);
        this.prefix = prefix;
        this.method = method;
        OpenLArgumentsCloner cloner = new OpenLArgumentsCloner();
        if (params != null) {
            Object[] clonedParams;
            try {
                clonedParams = cloner.deepClone(params);
            } catch (Exception ex) {
                // ignore cloning exception if any, use params itself
                //
                clonedParams = params;
            }
            this.params = clonedParams;
        } else {
            this.params = new Object[0];
        }
    }

    public ExecutableRulesMethod getTraceObject() {
        if (method != null) {
            return method;
        }
        ITracerObject parent = getParent();
        if (parent instanceof ATableTracerNode) {
            return ((ATableTracerNode)parent).getTraceObject();
        }
        throw new IllegalStateException("The executable method is not defined");
    }

    public String getPrefix() {
        return prefix;
    }

    public Object[] getParameters() {
        return params;
    }

    @Override
    public String getUri() {
        return getTraceObject().getSourceUrl();
    }
}
