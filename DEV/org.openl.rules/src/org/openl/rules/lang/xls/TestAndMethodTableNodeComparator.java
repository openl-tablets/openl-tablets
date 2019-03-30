package org.openl.rules.lang.xls;

import java.util.Comparator;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class TestAndMethodTableNodeComparator implements Comparator<TableSyntaxNode> {

    @Override
    public int compare(TableSyntaxNode tableSyntaxNode1, TableSyntaxNode tableSyntaxNode2) {

        String type1 = tableSyntaxNode1.getType();
        String type2 = tableSyntaxNode2.getType();

        int i1 = isTestMethod(type1) || isRunMethod(type1) ? 1 : 0;
        int i2 = isTestMethod(type2) || isRunMethod(type2) ? 1 : 0;

        return i1 - i2;
    }

    private boolean isTestMethod(String type) {
        return XlsNodeTypes.XLS_TEST_METHOD.toString().equals(type);
    }

    private boolean isRunMethod(String type) {
        return XlsNodeTypes.XLS_RUN_METHOD.toString().equals(type);
    }
}
