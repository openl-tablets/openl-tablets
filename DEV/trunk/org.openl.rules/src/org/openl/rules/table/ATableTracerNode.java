package org.openl.rules.table;

import org.apache.commons.lang.StringUtils;
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
       
        buf.append(resultAsString(method));
//        Temporary commented buf.append(parametersAsString(method, mode));
        
        buf.append(method.getName()).append('(').append(method.getSignature().toString()).append(')');
        
        // buf.append(MethodUtil.printMethod(getDT(), IMetaInfo.REGULAR,
        // false));
        return buf.toString();
    }

    protected String resultAsString(IOpenMethod method) {
        StringBuffer buf = new StringBuffer(64);
        if (!isVoid(method)) {                        
            if (hasError()) {
                // append error of any
                //
                buf.append(ERROR_RESULT);
            } else {
                // append formatted result
                //
                buf.append(getFormattedValue(result, method));
            }
            buf.append(' ');
        }
        return buf.toString();
    }

    protected String parametersAsString(IOpenMethod method, int mode) {
        StringBuffer buf = new StringBuffer(64);
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
        return buf.toString();
    }

    protected boolean isVoid(IOpenMethod method) {
        return (method.getType() == JavaOpenClass.VOID);        
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
    
    public String getFormattedResult() {
        IFormatter formatter = FormattersManager.getFormatter(result);
        if (formatter != null) {
            return formatter.format(result);
        }
        return StringUtils.EMPTY;        
    }
    
    protected String getFormattedValue(Object value, IOpenMethod method) {
        StringBuffer buf = new StringBuffer(28);
        // add '=' symbol if not void
        //
        buf.append("= ");
        IFormatter formatter = FormattersManager.getFormatter(value);
        String strValue = null;
        if (formatter != null) {
            buf.append(formatter.format(value));
        } else {
            buf.append(String.valueOf(value));
        }
        return buf.toString();
    }

}
