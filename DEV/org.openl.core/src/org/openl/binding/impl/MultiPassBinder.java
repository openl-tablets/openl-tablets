package org.openl.binding.impl;

import java.util.Iterator;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.util.OpenIterator;

public class MultiPassBinder {

    static public abstract class MultiPass {
        public abstract void makePass(MultiPassBinder mpbinder, IBoundNode node, IBindingContext cxt);

        public Iterator<IBoundNode> orderNodes(IBoundNode[] nodes) {
            return OpenIterator.fromArray(nodes);
        }

    }

    public void bind(MultiPass[] passes, IBoundNode[] nodes, IBindingContext cxt) {
        for (int i = 0; i < passes.length; i++) {
            makePass(passes[i], nodes, cxt);
        }
    }

    protected void makePass(MultiPass pass, IBoundNode[] nodes, IBindingContext cxt) {
        for (Iterator<IBoundNode> ordered = pass.orderNodes(nodes); ordered.hasNext();) {
            IBoundNode node = ordered.next();
            pass.makePass(this, node, cxt);
        }

    }

}
