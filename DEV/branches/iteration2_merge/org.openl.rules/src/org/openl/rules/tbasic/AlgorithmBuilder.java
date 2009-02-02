package org.openl.rules.tbasic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.meta.StringValue;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
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
        IGridTable grid = tableBody.getGridTable();

        Map<String, AlgorithmColumn> columns = new HashMap<String, AlgorithmColumn>();
        // parse ids, row=0
        for (int c = 0; c < grid.getLogicalWidth(); c++) {
            String id = grid.getStringValue(c, 0);

            if (columns.get(id) != null) {
                // duplicate ids
            }

            columns.put(id, new AlgorithmColumn(id, c));
        }

        // parse data, row=2..*
        for (int r = 0; r < grid.getLogicalHeight(); r++) {
            
            AlgorithmRow aRow = new AlgorithmRow();
            // parse data row
            for (AlgorithmColumn column : columns.values()) {
                int c = column.columnIndex;
                
                String value = grid.getStringValue(c, r);
                String uri = grid.getUri(c, r);

                StringValue sv = new StringValue(value, "cell" + r + "_" + c, null, uri);
                
                aRow.set(column.id, sv);
                if ("Operation".equalsIgnoreCase(column.id)) {
                    int i = grid.getCellStyle(c, r).getIdent();
                    aRow.setOperationLevel(i);
                }
            }
            
            algorithm.addRow(aRow);
        }
    }

    // Section Description Operation Condition Action Before After
    private static class AlgorithmColumn {
        private String id;
        private int columnIndex;

        public AlgorithmColumn(String id, int columnIndex) {
            this.id = id;
            this.columnIndex = columnIndex;
        }
    }
}
