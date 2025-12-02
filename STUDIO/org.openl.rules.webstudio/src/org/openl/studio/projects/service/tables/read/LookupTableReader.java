package org.openl.studio.projects.service.tables.read;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.stereotype.Component;

import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.ITable;
import org.openl.studio.projects.model.tables.LookupHeaderView;
import org.openl.studio.projects.model.tables.LookupView;
import org.openl.studio.projects.service.tables.OpenLTableUtils;

/**
 * Reads {@code SmartLookup} table to {@link LookupView} model.
 *
 * @author Vladyslav Pikus
 */
@Component
public class LookupTableReader extends ExecutableTableReader<LookupView, LookupView.Builder> {

    public LookupTableReader() {
        super(LookupView::builder);
    }

    @Override
    public boolean supports(IOpenLTable table) {
        return OpenLTableUtils.isSmartLookup(table) || OpenLTableUtils.isSimpleLookup(table);
    }

    @Override
    protected void initialize(LookupView.Builder builder, IOpenLTable openLTable) {
        super.initialize(builder, openLTable);

        var tsn = openLTable.getSyntaxNode();
        var tableBody = tsn.getTableBody();

        var headerTable = tableBody.getRow(0);
        var cellValueReader = new CellValueReader(tsn.getMetaInfoReader());
        var headers = buildHeaders(headerTable);
        builder.headers(headers);

        processRows(builder,
                headers,
                tableBody.getSubtable(0, 1, tableBody.getWidth(), tableBody.getHeight() - 1),
                cellValueReader);
    }

    /**
     * Builds hierarchical header structure from the header row(s).
     * The header may span multiple rows if there are hierarchical child headers.
     */
    public static List<LookupHeaderView> buildHeaders(ILogicalTable headerTable) {
        List<LookupHeaderView> headers = new ArrayList<>();
        var width = OpenLTableUtils.getWidthWithoutEmptyColumns(headerTable);
        for (int colNum = 0; colNum < width; colNum++) {
            var column = headerTable.getColumn(colNum);
            var headerBuilder = LookupHeaderView.builder()
                    .title(column.getCell(0, 0).getStringValue());
            var colHeight = OpenLTableUtils.getHeightWithoutEmptyRows(column);
            if (colHeight > 1) {
                List<LookupHeaderView> subHeaders = new ArrayList<>();
                for (int rowNum = 1; rowNum < colHeight; rowNum++) {
                    var subHeader = column.getRow(rowNum);
                    subHeaders.addAll(buildHeaders(subHeader));
                }
                headerBuilder.children(subHeaders);
            }
            headers.add(headerBuilder.build());
        }
        return headers;
    }

    /**
     * Processes data rows and builds the nested structure matching the header hierarchy.
     */
    private void processRows(LookupView.Builder builder,
                             List<LookupHeaderView> headers,
                             ILogicalTable rowsTable,
                             CellValueReader cellValueReader) {
        var list = new ArrayList<LinkedHashMap<String, Object>>();
        var sourceTable = rowsTable.getSource();
        var height = OpenLTableUtils.getHeightWithoutEmptyRows(sourceTable);

        for (int rowId = 0; rowId < height; rowId++) {
            var row = new LinkedHashMap<String, Object>();
            int colId = 0;

            for (var header : headers) {
                Object value = readHeaderValue(sourceTable, header, colId, rowId, cellValueReader);
                row.put(header.title, value);
                colId += getHeaderColumnCount(header);
            }

            list.add(row);
        }

        builder.rows(list);
    }

    /**
     * Reads a value for a header, building nested structure if the header has children.
     */
    private Object readHeaderValue(ITable<?> sourceTable,
                                   LookupHeaderView header,
                                   int startCol,
                                   int rowId,
                                   CellValueReader cellValueReader) {
        if (header.children.isEmpty()) {
            // Leaf header - read single cell
            var cell = sourceTable.getCell(startCol, rowId);
            return cellValueReader.apply(cell);
        } else {
            // Parent header - build nested map from children
            var nestedMap = new LinkedHashMap<String, Object>();
            int col = startCol;

            for (var child : header.children) {
                Object childValue = readHeaderValue(sourceTable, child, col, rowId, cellValueReader);
                nestedMap.put(child.title, childValue);
                col += getHeaderColumnCount(child);
            }

            return nestedMap;
        }
    }

    /**
     * Gets the total column count for a header and all its descendants.
     */
    private int getHeaderColumnCount(LookupHeaderView header) {
        if (header.children.isEmpty()) {
            return 1;
        }
        return header.children.stream().mapToInt(this::getHeaderColumnCount).sum();
    }
}
