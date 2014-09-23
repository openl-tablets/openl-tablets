package org.openl.rules.table;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.trace.SimpleTracerObject;

public abstract class ATableTracerNode extends SimpleTracerObject implements ITableTracerObject {

    public static final String ERROR_RESULT = "ERROR";

    private Object params[];
    private Throwable error;

    public ATableTracerNode() {
        this(null, null);
    }

    public ATableTracerNode(IMemberMetaInfo traceObject, Object[] params) {
        /**
         * Why traceObject is instanceof IMemberMetaInfo? don`t need it!
         * TODO: refactor change traceObject instance. Seems it should be ExecutableRulesMethod instance.
         * @author DLiauchuk
         */
        super(traceObject);
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

    protected String asString(IOpenMethod method, int mode) {
        StringBuilder buf = new StringBuilder(64);
        buf.append(method.getType().getDisplayName(mode)).append(' ');

        buf.append(resultAsString(method));

        buf.append(method.getName()).append('(').append(method.getSignature().toString()).append(')');

        return buf.toString();
    }

    protected String resultAsString(IOpenMethod method) {
        StringBuilder buf = new StringBuilder(64);
        if (!isVoid(method)) {
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
        return buf.toString();
    }

    protected String parametersAsString(IOpenMethod method, int mode) {
        StringBuilder buf = new StringBuilder(64);
        buf.append(method.getName()).append('(');

        IOpenClass[] paramTypes = method.getSignature().getParameterTypes();

        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(paramTypes[i].getDisplayName(mode)).append(' ');
            buf.append(method.getSignature().getParameterName(i)).append(" = ");
            buf.append(FormattersManager.format(params[i]));
        }
        buf.append(')');
        return buf.toString();
    }

    protected boolean isVoid(IOpenMethod method) {
        return (JavaOpenClass.isVoid(method.getType()));
    }

    public TableSyntaxNode getTableSyntaxNode() {
        TableSyntaxNode syntaxNode = null;

        IMemberMetaInfo tracedNode = (IMemberMetaInfo) getTraceObject();
        if (tracedNode != null) {
            ISyntaxNode tsn = tracedNode.getSyntaxNode();
            if (tsn instanceof TableSyntaxNode) {
                syntaxNode = (TableSyntaxNode) tsn;
            }
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

    public String getFormattedResult() {
        return FormattersManager.format(getResult());
    }

    protected String getFormattedValue(Object value, IOpenMethod method) {
        // add '=' symbol if not void
        return "= " + FormattersManager.format(value);
    }

}
