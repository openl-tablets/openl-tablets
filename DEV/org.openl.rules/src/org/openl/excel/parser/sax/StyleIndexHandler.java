package org.openl.excel.parser.sax;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.util.CellAddress;
import org.openl.rules.table.IGridRegion;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class StyleIndexHandler extends DefaultHandler {
    private final IGridRegion tableRegion;
    private final int[][] cellIndexes;
    private final Map<CellAddress, String> formulas = new HashMap<>();

    private CellAddress current;
    private boolean readFormula;
    private StringBuilder formula = new StringBuilder();

    public StyleIndexHandler(IGridRegion tableRegion) {
        this.tableRegion = tableRegion;
        cellIndexes = new int[IGridRegion.Tool.height(tableRegion)][IGridRegion.Tool.width(tableRegion)];
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("c".equals(localName)) {
            String cellRef = attributes.getValue("r");
            current = new CellAddress(cellRef);
            if (IGridRegion.Tool.contains(tableRegion, current.getColumn(), current.getRow())) {
                String cellStyleStr = attributes.getValue("s");
                int styleIndex = cellStyleStr != null ? Integer.parseInt(cellStyleStr) : 0;
                int internalRow = current.getRow() - tableRegion.getTop();
                int internalCol = current.getColumn() - tableRegion.getLeft();
                cellIndexes[internalRow][internalCol] = styleIndex;
            }
        } else if ("f".equals(localName)) {
            if (IGridRegion.Tool.contains(tableRegion, current.getColumn(), current.getRow())) {
                readFormula = true;
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (readFormula) {
            formula.append(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if ("f".equals(localName)) {
            readFormula = false;
            if (IGridRegion.Tool.contains(tableRegion, current.getColumn(), current.getRow())) {
                formulas.put(current, formula.toString());
            }
            formula.setLength(0);
        }
    }

    public int[][] getCellIndexes() {
        return cellIndexes;
    }

    public Map<CellAddress, String> getFormulas() {
        return formulas;
    }
}
