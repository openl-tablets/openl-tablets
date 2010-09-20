package org.openl.rules.cmatch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.binding.IBindingContext;
import org.openl.meta.StringValue;
import org.openl.rules.cmatch.algorithm.IMatchAlgorithmCompiler;
import org.openl.rules.cmatch.algorithm.MatchAlgorithmFactory;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.OffSetGridTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;

public class ColumnMatchBuilder {
    private final IBindingContext bindingContext;
    private final ColumnMatch columnMatch;
    private final TableSyntaxNode tsn;

    private List<TableColumn> columns;

    public ColumnMatchBuilder(IBindingContext ctx, ColumnMatch columnMatch, TableSyntaxNode tsn) {
        bindingContext = ctx;
        this.columnMatch = columnMatch;
        this.tsn = tsn;
    }

    public void build(IGridTable tableBody) throws Exception {
        if (tableBody.getGridHeight() < 4) {
            throw SyntaxNodeExceptionUtils.createError("Unsufficient rows. At least 4 are expected!", null, tsn);
        }

        prepareColumns(tableBody);

        List<TableRow> rows = buildRows(tableBody);

        columnMatch.setColumns(columns);
        columnMatch.setRows(rows);

        IOpenSourceCodeModule alg = columnMatch.getAlgorithm();
        String nameOfAlgorithm = (alg != null) ? alg.getCode() : null;
        IMatchAlgorithmCompiler algorithm = null;
        try {
            algorithm = MatchAlgorithmFactory.getAlgorithm(nameOfAlgorithm);
        } catch (Exception ex) {
            throw SyntaxNodeExceptionUtils.createError(null, ex, columnMatch.getSyntaxNode());
        }

        algorithm.compile(bindingContext, columnMatch);
    }

    private List<TableRow> buildRows(IGridTable tableBody) throws SyntaxNodeException {
        IGridTable leftRows = tableBody.getColumn(0).rows(2);

        int dataRowsCount = leftRows.getGridHeight();

        // init rows
        List<TableRow> rows = new ArrayList<TableRow>(dataRowsCount);
        for (int i = 0; i < dataRowsCount; i++) {
            rows.add(new TableRow());
        }

        // fill all rows (per column)
        for (TableColumn column : columns) {
            IGridTable colTable = tableBody.getRegion(column.getColumnIndex(), 2, 1, 1);

            IGridTable data;
            if (column.getColumnIndex() == 0) {
                if (colTable.getGridWidth() != 1) {
                    throw SyntaxNodeExceptionUtils.createError("First column must have width=1!", null, tsn);
                }

                data = leftRows.rows(1);
            } else {
                data = OffSetGridTableHelper.mergeBounds(leftRows, colTable);
            }

            // fill rows of particular column
            // fills from 1th till last (0-th will be filled below)
            int subColumns = data.getGridWidth();
            IGridTable grid = data.getGridTable();

            for (int r = 0; r < data.getGridHeight(); r++) {
                SubValue[] values = createSV(column, grid, r, subColumns);
                rows.get(r + 1).add(column.getId(), values);
            }

            // 0-th row
            grid = colTable.getGridTable();
            SubValue[] values = createSV(column, grid, 0, subColumns);
            rows.get(0).add(column.getId(), values);
        }

        return rows;
    }

    private SubValue[] createSV(TableColumn column, IGridTable grid, int r, int subColumns) {
        SubValue[] values = new SubValue[subColumns];

        for (int c = 0; c < subColumns; c++) {
            String value = grid.getCell(c, r).getStringValue();

            if (value == null) {
                value = "";
            } else {
                // kill extra spaces -- fix type errors
                value = value.trim();
            }

            String cellName = "cell" + r + "_" + column.getColumnIndex() + "_" + c;
            StringValue sv = new StringValue(value, cellName, cellName, new GridCellSourceCodeModule(grid, c, r));
            values[c] = new SubValue(sv, grid.getCell(c, r).getStyle());
            IGridTable lr = grid.getRegion(c, r, 1, 1);
            values[c].setGridRegion(lr.getGridTable().getRegion());
        }

        return values;
    }    
    
    private void prepareColumns(IGridTable tableBody) throws SyntaxNodeException {
        columns = new ArrayList<TableColumn>();
        Set<String> addedIds = new HashSet<String>();

        IGridTable ids = tableBody.getRow(0);

        // parse ids, row=0
        for (int c = 0; c < ids.getGridWidth(); c++) {
            String id = safeId(ids.getColumn(c).getGridTable().getCell(0, 0).getStringValue());
            if (id.length() == 0) {
                // ignore column with NO ID
                continue;
            }

            if (addedIds.contains(id)) {
                // duplicate ids
                throw SyntaxNodeExceptionUtils.createError("Duplicate column '" + id + "'!", null, tsn);
            }

            columns.add(new TableColumn(id, c));
            addedIds.add(id);
        }
    }

    private String safeId(String s) {
        if (s == null) {
            return "";
        }
        return (s.trim().toLowerCase());
    }
}
