package org.openl.rules.table;

import org.openl.base.INamedThing;
import org.openl.types.IOpenMethod;
import org.openl.util.print.Formatter;
import org.openl.vm.ITracerObject;

public abstract class ATableTracerNode extends ITracerObject.SimpleTracerObject implements ITableTracerObject {
    private Object params[];
    
    public ATableTracerNode() {
    }

    public ATableTracerNode(IOpenMethod traceObject, Object[] params) {
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
}
