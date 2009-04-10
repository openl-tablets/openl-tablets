package org.openl.rules.table;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.print.Formatter;
import org.openl.vm.ITracerObject;

public abstract class ATableTracerNode extends ITracerObject.SimpleTracerObject implements ITableTracerObject {
    private Object params[];
    private Object result;
    
    public ATableTracerNode() {
    }

    public ATableTracerNode(IMemberMetaInfo traceObject, Object[] params) {
        super(traceObject);
        this.params = params;
    }

    public ITableTracerObject[] getTableTracers() {
        ITracerObject[] tracerObjects = getTracerObjects();

        int size = tracerObjects.length;
        ITableTracerObject[] temp = new ITableTracerObject[size];

        System.arraycopy(tracerObjects, 0, temp, 0, size);

        return temp;
    }

    protected String asString(IOpenMethod method, int mode) {
        StringBuffer buf = new StringBuffer(64);
        buf.append(method.getType().getDisplayName(mode)).append(' ');
        boolean isVoidReturnType = (method.getType() == JavaOpenClass.VOID);
        if (!isVoidReturnType) {
            buf.append("= ").append(result != null ? result.toString() : "null").append(' ');
        }
        buf.append(method.getName()).append('(');
        
        IOpenClass[] paramTypes = method.getSignature().getParameterTypes();

        for (int i = 0; i < params.length; i++)
        {
            if (i > 0)
                buf.append(", ");
            buf.append(paramTypes[i].getDisplayName(mode)).append(' ');
            buf.append(method.getSignature().getParameterName(i)).append(" = ");
            Formatter.format(params[i], mode, buf);
        }
        
        buf.append(')');
//      buf.append(MethodUtil.printMethod(getDT(), IMetaInfo.REGULAR, false));
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

    /**
     * @param result the result to set
     */
    public void setResult(Object result) {
        this.result = result;
    }

    /**
     * @return the result
     */
    public Object getResult() {
        return result;
    }
}
