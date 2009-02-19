package org.openl.rules.cmatch;

import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.OpenlTool;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IMemberBoundNode;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.SubTextSourceCodeModule;
import org.openl.syntax.impl.SyntaxError;
import org.openl.types.impl.OpenMethodHeader;

public class ColumnMatchNodeBinder extends AXlsTableBinder {
    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tsn, OpenL openl, IBindingContext cxt, XlsModuleOpenClass module)
            throws Exception {
        IGridTable table = tsn.getTable().getGridTable();

        IOpenSourceCodeModule src = new GridCellSourceCodeModule(table);

        int headerTokenLength = tsn.getHeader().getHeaderToken().getIdentifier().length();

        SubTextSourceCodeModule nameOfAlgorithm = cutNameOfAlgorithm(tsn, src, headerTokenLength);
        if (nameOfAlgorithm != null) {
            String name = nameOfAlgorithm.getCode();
            // TODO
            // headerTokenLength = name.getEndPosition() + 1;
            headerTokenLength = nameOfAlgorithm.getStartPosition() + name.length() + 1;
        }

        SubTextSourceCodeModule codeModule = new SubTextSourceCodeModule(src, headerTokenLength);
        OpenMethodHeader header = (OpenMethodHeader) OpenlTool.getMethodHeader(codeModule, openl,
                (IBindingContextDelegator) cxt);

        header.setDeclaringClass(module);

        return new ColumnMatchBoundNode(tsn, openl, header, module, nameOfAlgorithm);
    }

    private SubTextSourceCodeModule cutNameOfAlgorithm(TableSyntaxNode tsn, IOpenSourceCodeModule src,
            int headerTokenLength) throws SyntaxError {
        String s = src.getCode();

        // parse '<ALGORITHM>' if it exists
        int p2 = s.indexOf('>');
        if (p2 < 0) {
            return null;
        }

        int p1 = s.indexOf('<');
        for (int i = headerTokenLength; i < p1; i++) {
            if (s.charAt(i) != ' ') {
                // illegal character detected
                p1 = -1;
                break;
            }
        }

        if (p1 < 0 || p1 > p2) {
            throw new SyntaxError(tsn, "Illegal header format!", null);
        }

        return new SubTextSourceCodeModule(src, p1 + 1, p2);
    }
}
