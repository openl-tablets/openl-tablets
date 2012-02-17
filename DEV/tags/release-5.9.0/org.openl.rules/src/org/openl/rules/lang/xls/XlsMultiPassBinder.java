package org.openl.rules.lang.xls;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.MultiPassBinder;
import org.openl.rules.lang.xls.binding.ATableBoundNode;

public class XlsMultiPassBinder extends MultiPassBinder {

    static class ParseComponents extends XlsMultiPass {

        @Override
        public void makeXlsPass(XlsMultiPassBinder mpbinder, ATableBoundNode node, IBindingContext cxt) {
            node.parseComponents(cxt);
        }

    }

    static public abstract class XlsMultiPass extends MultiPass {

        @Override
        public void makePass(MultiPassBinder mpbinder, IBoundNode node, IBindingContext cxt) {
            makeXlsPass((XlsMultiPassBinder) mpbinder, (ATableBoundNode) node, cxt);

        }

        public abstract void makeXlsPass(XlsMultiPassBinder mpbinder, ATableBoundNode node, IBindingContext cxt);

    }

}
