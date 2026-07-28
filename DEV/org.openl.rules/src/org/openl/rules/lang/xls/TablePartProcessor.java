package org.openl.rules.lang.xls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.openl.GridTableSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;

public class TablePartProcessor {

    private final Collection<OpenLMessage> messages = new LinkedHashSet<>();

    /**
     * @return a list of TableParts with tables merged
     */
    public List<TablePart> mergeAllNodes() {
        var tables = new ArrayList<TablePart>();
        for (SortedSet<TablePart> set : tableParts.values()) {
            try {
                var mergedTable = validateAndMerge(set);
                tables.add(mergedTable);
            } catch (OpenLCompilationException e) {
                messages.add(OpenLMessagesUtils.newErrorMessage(e));
            }
        }

        return tables;
    }

    private TablePart validateAndMerge(SortedSet<TablePart> set) throws OpenLCompilationException {

        var cnt = 0;
        var n = set.size();

        IGridTable[] tables = new IGridTable[n];

        var vertical = false;
        var dimension = 0;
        TablePart first = null;

        for (TablePart tablePart : set) {

            if (tablePart.getPart() != cnt + 1) {
                var message = "TablePart number " + tablePart.getPart() + " is out of order";
                throw new OpenLCompilationException(message, null, null, makeSourceModule(tablePart.getTable()));
            }

            if (tablePart.getSize() != n) {
                var message = "TablePart " + tablePart.getPartName() + " number " + tablePart.getPart() + " has wrong number of parts: " + tablePart
                        .getSize() + ". There are " + n + " parts with the same name";
                throw new OpenLCompilationException(message, null, null, makeSourceModule(tablePart.getTable()));
            }

            var cell00 = tablePart.getTable().getCell(0, 0);

            var table = tablePart.getTable().getRows(cell00.getHeight());
            if (table == null) {
                var message = "TablePart " + tablePart.getPartName() + " number " + tablePart.getPart() + " has wrong content.";
                throw new OpenLCompilationException(message, null, null, makeSourceModule(tablePart.getTable()));

            }
            var myVert = tablePart.isVertical();
            int myDim = myVert ? table.getWidth() : table.getHeight();

            if (cnt == 0) {
                first = tablePart;
                vertical = myVert;
                dimension = myDim;
            } else {
                if (myVert != vertical) {
                    var message = "TablePart number " + tablePart.getPart() + " must use " + (vertical ?
                            "row" :
                            "column");
                    throw new OpenLCompilationException(message, null, null, makeSourceModule(tablePart.getTable()));
                }

                if (myDim != dimension) {
                    var message = "TablePart number " + tablePart.getPart() + " has " + (vertical ?
                            "width" :
                            "height") + " = " + myDim + " instead of " + dimension;
                    if (vertical) {
                        throw new OpenLCompilationException(message,
                                null,
                                null,
                                makeSourceModule(tablePart.getTable()));
                    } else {
                        messages.add(OpenLMessagesUtils.newErrorMessage(message));
                    }
                }
            }
            tables[cnt++] = table;
        }
        CompositeGrid grid;
        if (vertical) {
            grid = new CompositeGrid(tables, true);
        } else {
            // Ignore table header and headers from all tables except first and expand width of cells to table total
            // width if horizontal merge is used. This is special behaviour for table parts and special composite grid
            // is used here.
            grid = new HorizontalTablePartsCompositeGrid(tables);
        }

        var table = new GridTable(0, 0, grid.getHeight() - 1, grid.getWidth() - 1, grid);

        return new TablePart(table, first.source);
    }

    private static IOpenSourceCodeModule makeSourceModule(IGridTable table) {
        return new GridTableSourceCodeModule(table);
    }

    final Map<String, TreeSet<TablePart>> tableParts = new HashMap<>();

    public void register(IGridTable table, XlsSheetSourceCodeModule source) throws OpenLCompilationException {
        var tablePart = new TablePart(table, source);
        parseHeader(tablePart);
        addToParts(tablePart);
    }

    private static final Pattern PATTERN = Pattern
            .compile("\\w+\\s+(\\w+)\\s+(column|row)\\s+(\\d+)\\s+of\\s+(\\d+)\\s*($)");

    private void parseHeader(TablePart tablePart) throws OpenLCompilationException {

        var src = new GridCellSourceCodeModule(tablePart.getTable());

        var header = src.getCode();

        var m = PATTERN.matcher(header);

        if (!m.matches()) {
            var message = "Valid Syntax: TablePart <table_id> <row|column> <npart(1 to total_number_of_parts)> of <total_number_of_parts>";
            throw new OpenLCompilationException(message, null, null, makeSourceModule(tablePart.getTable()));
        }

        var tableId = m.group(1);
        var colOrRow = m.group(2);
        var npart = m.group(3);
        var totalParts = m.group(4);

        tablePart.setPartName(tableId);
        tablePart.setPart(Integer.parseInt(npart));
        tablePart.setSize(Integer.parseInt(totalParts));
        tablePart.setVertical(colOrRow.equals("row"));
    }

    private synchronized void addToParts(TablePart tablePart) throws OpenLCompilationException {
        var key = tablePart.getPartName();
        var set = tableParts.computeIfAbsent(key, e -> new TreeSet<>());
        var res = set.add(tablePart);
        if (!res) {
            var message = "Duplicated TablePart part # = " + tablePart.getPart();
            throw new OpenLCompilationException(message, null, null, makeSourceModule(tablePart.getTable()));
        }
    }

    public Collection<OpenLMessage> getMessages() {
        return messages;
    }
}
