package org.openl.excel.parser.sax;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.EvaluationName;
import org.apache.poi.ss.formula.FormulaParsingWorkbook;
import org.apache.poi.ss.formula.SheetIdentifier;
import org.apache.poi.ss.formula.ptg.Area3DPxg;
import org.apache.poi.ss.formula.ptg.NameXPxg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.Ref3DPxg;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Table;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;

class SAXFormulaParsingWorkbook implements FormulaParsingWorkbook {
    private static final String NOT_SUPPORTED_FORMULA_TYPE = "Not supported formula type";

    @Override
    public EvaluationName getName(String name, int sheetIndex) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_FORMULA_TYPE);
    }

    @Override
    public Name createName() {
        throw new UnsupportedOperationException(NOT_SUPPORTED_FORMULA_TYPE);
    }

    @Override
    public Table getTable(String name) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_FORMULA_TYPE);
    }

    @Override
    public Ptg getNameXPtg(String name, SheetIdentifier sheet) {
        if (sheet == null) {
            throw new UnsupportedOperationException(NOT_SUPPORTED_FORMULA_TYPE);
        }
        if (sheet._sheetIdentifier == null) {
            throw new UnsupportedOperationException(NOT_SUPPORTED_FORMULA_TYPE);
        }

        // Use the sheetname and process
        String sheetName = sheet._sheetIdentifier.getName();

        if (sheet._bookName != null) {
            throw new UnsupportedOperationException(NOT_SUPPORTED_FORMULA_TYPE);
        } else {
            return new NameXPxg(sheetName, name);
        }
    }

    @Override
    public Ptg get3DReferencePtg(CellReference cell, SheetIdentifier sheet) {
        if (sheet._bookName != null) {
            throw new UnsupportedOperationException(NOT_SUPPORTED_FORMULA_TYPE);
        } else {
            return new Ref3DPxg(sheet, cell);
        }
    }

    @Override
    public Ptg get3DReferencePtg(AreaReference area, SheetIdentifier sheet) {
        if (sheet._bookName != null) {
            throw new UnsupportedOperationException(NOT_SUPPORTED_FORMULA_TYPE);
        } else {
            return new Area3DPxg(sheet, area);
        }
    }

    @Override
    public int getExternalSheetIndex(String sheetName) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_FORMULA_TYPE);
    }

    @Override
    public int getExternalSheetIndex(String workbookName, String sheetName) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_FORMULA_TYPE);
    }

    @Override
    public SpreadsheetVersion getSpreadsheetVersion() {
        return SpreadsheetVersion.EXCEL2007;
    }
}
