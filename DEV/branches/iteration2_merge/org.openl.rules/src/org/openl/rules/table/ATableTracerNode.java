package org.openl.rules.table;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenMethod;
import org.openl.util.print.Formatter;
import org.openl.vm.ITracerObject;

public abstract class ATableTracerNode extends ITracerObject.SimpleTracerObject implements ITableTracerObject {
    private Object params[];
    
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

    protected String asString(IOpenMethod method) {
        StringBuffer buf = new StringBuffer(64);
        buf.append(method.getType().getDisplayName(SHORT)).append(' ');
        buf.append(method.getName()).append('(');

        for (int i = 0; i < params.length; i++)
        {
            if (i > 0)
                buf.append(", ");
            Formatter.format(params[i], INamedThing.SHORT, buf);
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
}
