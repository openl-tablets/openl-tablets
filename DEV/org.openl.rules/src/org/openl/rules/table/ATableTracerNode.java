package org.openl.rules.table;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.trace.SimpleTracerObject;

import java.util.List;

public abstract class ATableTracerNode extends SimpleTracerObject implements ITableTracerObject {

    public static final String ERROR_RESULT = "ERROR";

    private String type;
    private Object params[];
    private Throwable error;
    private ExecutableRulesMethod method;
    private String prefix;

    public ATableTracerNode(String type, String prefix, ExecutableRulesMethod method, Object[] params) {
        this.type = type;
        this.prefix = prefix;
        this.method = method;
        OpenLArgumentsCloner cloner = new OpenLArgumentsCloner();
        if (params != null) {
            Object[] clonedParams = null;
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

    @Override
    public String getDisplayName(int mode) {
        StringBuilder buf = new StringBuilder(64);
        buf.append(prefix).append(' ');
        IOpenClass type = method.getType();
        buf.append(type.getDisplayName(mode)).append(' ');

        if (!JavaOpenClass.isVoid(type)) {
            if (hasError()) {
                // append error of any
                //
                buf.append(ERROR_RESULT);
            } else {
                // append formatted result
                //
                buf.append(getFormattedValue(getResult(), method));
            }
            buf.append(' ');
        }

        buf.append(method.getName()).append('(').append(method.getSignature().toString()).append(')');

        return buf.toString();
    }

    public TableSyntaxNode getTableSyntaxNode() {
        TableSyntaxNode syntaxNode = null;
        if (method != null) {
            syntaxNode = method.getSyntaxNode();
        }
        return syntaxNode;
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

    protected String getFormattedValue(Object value, IOpenMethod method) {
        // add '=' symbol if not void
        return "= " + FormattersManager.format(value);
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getUri() {
        return method.getSourceUrl();
    }

    @Override
    public List<IGridRegion> getGridRegions() {
        // Default stub implementation
        return null;
    }
}
