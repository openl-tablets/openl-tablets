package org.openl.rules.table;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.formatters.IFormatter;
import org.openl.vm.trace.ITracerObject;
import org.openl.vm.trace.SimpleTracerObject;

public abstract class ATableTracerNode extends SimpleTracerObject implements ITableTracerObject {

    public static final String ERROR_RESULT = "ERROR";

    private Object params[];
    private Object result;
    private Throwable error;

    public ATableTracerNode() {
    }

    public ATableTracerNode(IMemberMetaInfo traceObject, Object[] params) {
    	/**
         * why traceObject is instnceof IMemberMetaInfo? don`t need it!
         * TODO: refactor change traceObject instance. Seems it should be ExecutableRulesMethod instance.
         * @author DLiauchuk
         */

        super(traceObject);
        this.params = params;
    }

    protected String asString(IOpenMethod method, int mode) {
        StringBuffer buf = new StringBuffer(64);
        buf.append(method.getType().getDisplayName(mode)).append(' ');
        boolean isVoidReturnType = (method.getType() == JavaOpenClass.VOID);
        if (!isVoidReturnType) {
            buf.append("= ");
            if (hasError()) {
                buf.append(ERROR_RESULT);
            } else {
                
                buf.append(getFormattedValue(result));
            }
            buf.append(' ');
        }
        buf.append(method.getName()).append('(');

        IOpenClass[] paramTypes = method.getSignature().getParameterTypes();

        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(paramTypes[i].getDisplayName(mode)).append(' ');
            buf.append(method.getSignature().getParameterName(i)).append(" = ");
            IFormatter formatter = FormattersManager.getFormatter(params[i]);
            buf.append(formatter.format(params[i]));
        }
        buf.append(')');
        // buf.append(MethodUtil.printMethod(getDT(), IMetaInfo.REGULAR,
        // false));
        return buf.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.rules.table.ITableTracerObject#getTableSyntaxNode()
     */
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

    public ITableTracerObject[] getTableTracers() {
        ITracerObject[] tracerObjects = getTracerObjects();

        int size = tracerObjects.length;
        ITableTracerObject[] temp = new ITableTracerObject[size];

        System.arraycopy(tracerObjects, 0, temp, 0, size);

        return temp;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
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
    
    private String getFormattedValue(Object value) {
        IFormatter formatter = FormattersManager.getFormatter(value);
        String strValue = null;
        if (formatter != null) {
            strValue = formatter.format(value);
        } else {
            strValue = String.valueOf(value);
        }
        return strValue;
    }

}
