package org.openl.excel.parser.sax;

import org.apache.poi.ss.util.CellAddress;
import org.openl.rules.table.IGridRegion;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class StyleIndexHandler extends DefaultHandler {
    private final IGridRegion tableRegion;
    private final int[][] cellIndexes;

    public StyleIndexHandler(IGridRegion tableRegion) {
        this.tableRegion = tableRegion;
        cellIndexes = new int[IGridRegion.Tool.height(tableRegion)][IGridRegion.Tool.width(tableRegion)];
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("c".equals(localName)) {
            String cellRef = attributes.getValue("r");
            CellAddress current = new CellAddress(cellRef);
            if (IGridRegion.Tool.contains(tableRegion, current.getColumn(), current.getRow())) {
                String cellStyleStr = attributes.getValue("s");
                int styleIndex = cellStyleStr != null ? Integer.parseInt(cellStyleStr) : 0;
                int internalRow = current.getRow() - tableRegion.getTop();
                int internalCol = current.getColumn() - tableRegion.getLeft();
                cellIndexes[internalRow][internalCol] = styleIndex;
            }
        }
    }

    public int[][] getCellIndexes() {
        return cellIndexes;
    }
}
