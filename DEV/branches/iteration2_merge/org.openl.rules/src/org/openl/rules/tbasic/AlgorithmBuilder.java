package org.openl.rules.tbasic;

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
        Map<String, AlgorithmColumn> columns = new HashMap<String, AlgorithmColumn>();
        
        ILogicalTable ids = tableBody.getLogicalRow(0);
        
        // parse ids, row=0
        for (int c = 0; c < ids.getLogicalWidth(); c++) {
            String id = ids.getGridTable().getStringValue(c, 0);
            id = id.toLowerCase();

            if (columns.get(id) != null) {
                // duplicate ids
                throw new IllegalStateException("Duplicate column '" + id + "'!");
            }

            columns.put(id, new AlgorithmColumn(id, c));
        }

        // parse data, row=2..*
        IGridTable grid = tableBody.rows(2).getGridTable();
        for (int r = 0; r < grid.getLogicalHeight(); r++) {

            AlgorithmRow aRow = new AlgorithmRow();
            // parse data row
            for (AlgorithmColumn column : columns.values()) {
                int c = column.columnIndex;

                String value = grid.getStringValue(c, r);
                String uri = grid.getUri(c, r);

                if (value == null) {
                    value = "";
                }

                StringValue sv = new StringValue(value, "cell" + r + "_" + c, null, uri);

                setRowField(aRow, column.id, sv);
                if ("Operation".equalsIgnoreCase(column.id)) {
                    int i = grid.getCellStyle(c, r).getIdent();
                    aRow.setOperationLevel(i);
                }
            }

            algorithm.addRow(aRow);
        }
    }

    private void setRowField(AlgorithmRow row, String column, StringValue sv) {
        if ("section".equalsIgnoreCase(column)) {
            row.setLabel(sv);
        } else if ("description".equalsIgnoreCase(column)) {
            row.setDescription(sv);
        } else if ("operation".equalsIgnoreCase(column)) {
            row.setOperation(sv);
        } else if ("condition".equalsIgnoreCase(column)) {
            row.setCondition(sv);
        } else if ("action".equalsIgnoreCase(column)) {
            row.setAction(sv);
        } else if ("before".equalsIgnoreCase(column)) {
            row.setBefore(sv);
        } else if ("after".equalsIgnoreCase(column)) {
            row.setAfter(sv);
        } else {
            throw new IllegalArgumentException("Invalid column id '" + column + "'!");
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
