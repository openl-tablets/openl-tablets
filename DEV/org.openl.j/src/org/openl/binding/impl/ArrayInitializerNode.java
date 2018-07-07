package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class ArrayInitializerNode extends ABoundNode {

    IOpenClass type;

    IOpenCast[] casts;

    public ArrayInitializerNode(ISyntaxNode syntaxNode, IBoundNode[] children, IOpenClass type, IOpenCast[] casts) {
        super(syntaxNode, children);
        this.type = type;
        this.casts = casts;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        IAggregateInfo info = type.getAggregateInfo();

        Object ary = info.makeIndexedAggregate(info.getComponentType(type), children.length);

        IOpenIndex index = info.getIndex(type, JavaOpenClass.INT);

        for (int i = 0; i < children.length; i++) {
            Object obj = children[i].evaluate(env);
            if (casts[i] != null) {
                obj = casts[i].convert(obj);
            }
            index.setValue(ary, i, obj);
        }

        return ary;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getType()
     */
    public IOpenClass getType() {
        return type;
    }

}
