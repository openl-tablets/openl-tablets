package org.openl.rules.calc;

import org.openl.OpenL;
import org.openl.binding.IMemberBoundNode;
import org.openl.rules.lang.xls.binding.AExecutableNodeBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.impl.OpenMethodHeader;

public class SSheetNodeBinder extends AExecutableNodeBinder {
    @Override
    protected IMemberBoundNode createNode(TableSyntaxNode tsn, OpenL openl, OpenMethodHeader header,
            XlsModuleOpenClass module) {
        return new SSheetBoundNode(tsn, openl, header, module);
    }
}
