package org.openl.rules.tbasic;

import org.openl.binding.IBindingContext;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;

public class AlgorithmBuilder {

    private final IBindingContext cxt;
    private final Algorithm algorithm;
    private final TableSyntaxNode tsn;

    public AlgorithmBuilder(IBindingContext cxt, Algorithm algorithm, TableSyntaxNode tsn) {
        this.cxt = cxt;
        this.algorithm = algorithm;
        this.tsn = tsn;
    }

    public void build(ILogicalTable tableBody) {
        ILogicalTable columnIdTable = tableBody.getLogicalRow(0).columns(0);
        ILogicalTable columnTitleTable = tableBody.getLogicalRow(0).columns(1);
        
        // FIXME
    }

//    Section Description Operation Condition Action Before After
    private static class AlgorithmColumn {
        private int columnIndex;
        private String id;
    }

    private static class AlgorithmRow {
        private String name;
        private String description;
        private String operation;
        private String condition;
        private String action;
        private String before;
        private String after;
    }
}
