package org.openl.rules.cmatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.binding.IBindingContext;
import org.openl.meta.StringValue;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTable;
import org.openl.syntax.impl.SyntaxError;

public class ColumnMatchBuilder {
    private final IBindingContext bindingContext;
    private final ColumnMatch columnMatch;
    private final TableSyntaxNode tsn;

    private List<TableColumn> columns;

    public ColumnMatchBuilder(IBindingContext ctx, ColumnMatch columnMatch, TableSyntaxNode tsn) {
        this.bindingContext = ctx;
        this.columnMatch = columnMatch;
        this.tsn = tsn;
    }

    public void build(ILogicalTable tableBody) throws Exception {
        if (tableBody.getLogicalHeight() <= 4) {
            throw new SyntaxError(tsn, "Unsufficient rows. At least 4 are expected!", null);
        }

        prepareColumns(tableBody);

        List<TableRow> rows = buildRows(tableBody);

        columnMatch.setColumns(columns);
        columnMatch.setRows(rows);

        // TODO compile
    }

    private void prepareColumns(ILogicalTable tableBody) throws SyntaxError {
        columns = new ArrayList<TableColumn>();
        Set<String> addedIds = new HashSet<String>();

        ILogicalTable ids = tableBody.getLogicalRow(0);

        // parse ids, row=0
        for (int c = 0; c < ids.getLogicalWidth(); c++) {
            String id = safeId(ids.getGridTable().getStringValue(c, 0));
            if (id.length() == 0) {
                // ignore column with NO ID
                continue;
            }

            if (addedIds.contains(id)) {
                // duplicate ids
                throw new SyntaxError(tsn, "Duplicate column '" + id + "'!", null);
            }

            columns.add(new TableColumn(id, c));
            addedIds.add(id);
        }
    }

    private List<TableRow> buildRows(ILogicalTable tableBody) throws SyntaxError {
        ILogicalTable leftRows = tableBody.getLogicalColumn(0).rows(2);

        int dataRowsCount = leftRows.getLogicalHeight();

        // init rows
        List<TableRow> rows = new ArrayList<TableRow>(dataRowsCount);
        for (int i = 0; i < dataRowsCount; i++) {
            rows.add(new TableRow());
        }

        // fill all rows (per column)
        for (TableColumn column : columns) {
            ILogicalTable colTable = tableBody.getLogicalRegion(column.getColumnIndex(), 2, 1, 1);

            ILogicalTable data;
            if (column.getColumnIndex() == 0) {
                if (colTable.getLogicalWidth() != 1) {
                    throw new SyntaxError(tsn, "First column must have width=1!", null);
                }

                data = leftRows.rows(1);
            } else {
                data = LogicalTable.mergeBounds(leftRows, colTable);
            }

            // fill rows of particular column
            // fills from 1th till last (0-th will be filled below)
            int subColumns = data.getLogicalWidth();
            IGridTable grid = data.getGridTable();

            for (int r = 0; r < data.getLogicalHeight(); r++) {
                SubValue[] values = new SubValue[subColumns];

                for (int c = 0; c < subColumns; c++) {
                    String value = grid.getStringValue(c, r);
                    String uri = grid.getUri(c, r);

                    if (value == null) {
                        value = "";
                    }

                    StringValue sv = new StringValue(value, "cell" + r + "_" + column.getColumnIndex() + "_" + c, null,
                            uri);
                    values[c] = new SubValue(sv, grid.getCellStyle(c, r).getIdent());
                }

                rows.get(r + 1).add(column.getId(), values);
            }

            // 0-th row
            grid = colTable.getGridTable();
            SubValue[] values = new SubValue[subColumns];
            for (int c = 0; c < subColumns; c++) {
                String value = grid.getStringValue(c, 0);
                String uri = grid.getUri(c, 0);

                if (value == null) {
                    value = "";
                }

                StringValue sv = new StringValue(value, null, null, uri);
                values[c] = new SubValue(sv, grid.getCellStyle(c, 0).getIdent());
            }

            rows.get(0).add(column.getId(), values);
        }

        return rows;
    }

    private String safeId(String s) {
        if (s == null)
            return "";
        return (s.trim().toLowerCase());
    }
}
