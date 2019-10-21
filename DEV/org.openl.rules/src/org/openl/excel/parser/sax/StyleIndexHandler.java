package org.openl.excel.parser.sax;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaRenderer;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.SharedFormula;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.openl.rules.table.IGridRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class StyleIndexHandler extends DefaultHandler {
    private final Logger log = LoggerFactory.getLogger(StyleIndexHandler.class);

    private final IGridRegion tableRegion;
    private final int[][] cellIndexes;
    private final Map<CellAddress, String> formulas = new HashMap<>();
    private final int sheetIndex;

    private CellAddress current;
    private boolean readFormula;
    private StringBuilder formula = new StringBuilder();
    private Map<String, SharedFormulaDefinition> sharedFormulas = new HashMap<>();
    private String sharedFormulaIndex;
    private String sharedFormulaRef;
    private SAXFormulaParsingWorkbook formulaParsingWorkbook;

    public StyleIndexHandler(IGridRegion tableRegion, int sheetIndex) {
        this.tableRegion = tableRegion;
        cellIndexes = new int[IGridRegion.Tool.height(tableRegion)][IGridRegion.Tool.width(tableRegion)];
        this.sheetIndex = sheetIndex;

        formulaParsingWorkbook = new SAXFormulaParsingWorkbook();
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
            sharedFormulaIndex = attributes.getValue("si");
            sharedFormulaRef = attributes.getValue("ref");
            if (IGridRegion.Tool.contains(tableRegion,
                current.getColumn(),
                current.getRow()) || sharedFormulaIndex != null && sharedFormulaRef != null) {
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
            if (sharedFormulaIndex != null && sharedFormulaRef != null) {
                sharedFormulas.put(sharedFormulaIndex,
                    new SharedFormulaDefinition(formula.toString(), sharedFormulaRef));
            }
            if (IGridRegion.Tool.contains(tableRegion, current.getColumn(), current.getRow())) {
                try {
                    String value = formula.toString();
                    if (sharedFormulaIndex != null && sharedFormulaRef == null) {
                        value = convertSharedFormula(sharedFormulas.get(sharedFormulaIndex));
                    }
                    formulas.put(current, value);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
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

    private String convertSharedFormula(SharedFormulaDefinition formulaDefinition) {
        CellRangeAddress ref = CellRangeAddress.valueOf(formulaDefinition.getRef());

        SharedFormula sf = new SharedFormula(SpreadsheetVersion.EXCEL2007);
        Ptg[] parsedTokens = FormulaParser.parse(formulaDefinition
            .getValue(), formulaParsingWorkbook, FormulaType.CELL, sheetIndex, current.getRow());
        Ptg[] convertedTokens = sf.convertSharedFormulas(parsedTokens,
            current.getRow() - ref.getFirstRow(),
            current.getColumn() - ref.getFirstColumn());
        // Formulas with links to other workbooks are not supported
        return FormulaRenderer.toFormulaString(null, convertedTokens);
    }

    private static class SharedFormulaDefinition {
        private final String value;
        private final String ref;

        private SharedFormulaDefinition(String value, String ref) {
            this.value = value;
            this.ref = ref;
        }

        public String getValue() {
            return value;
        }

        public String getRef() {
            return ref;
        }
    }

}
