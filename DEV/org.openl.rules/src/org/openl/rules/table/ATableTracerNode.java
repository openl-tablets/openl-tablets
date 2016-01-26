package org.openl.rules.table;

import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.vm.trace.SimpleTracerObject;

public abstract class ATableTracerNode extends SimpleTracerObject implements ITableTracerObject {

    private Object params[];
    private Throwable error;
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
        return method;
    }

    public String getPrefix() {
        return prefix;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public boolean hasError() {
        return error != null;
    }

    public Object[] getParameters() {
        return params.clone();
    }

    @Override
    public String getUri() {
        return method.getSyntaxNode().getUri();
    }
}
