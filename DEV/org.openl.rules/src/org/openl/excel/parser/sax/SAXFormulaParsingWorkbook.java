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
        // No defined names are tracked by the SAX parser. Returning null lets POI's FormulaParser
        // fall back to creating a NameXPxg for unknown identifiers (e.g. Analysis ToolPak functions
        // like RANDBETWEEN that are not in POI's built-in function registry).
        return null;
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
            // Unknown unscoped name — let POI's FormulaParser create a NameXPxg fallback.
            return null;
        }
        if (sheet.getSheetIdentifier() == null) {
            throw new UnsupportedOperationException(NOT_SUPPORTED_FORMULA_TYPE);
        }

        // Use the sheetname and process
        String sheetName = sheet.getSheetIdentifier().getName();

        if (sheet.getBookName() != null) {
            throw new UnsupportedOperationException(NOT_SUPPORTED_FORMULA_TYPE);
        } else {
            return new NameXPxg(sheetName, name);
        }
    }

    @Override
    public Ptg get3DReferencePtg(CellReference cell, SheetIdentifier sheet) {
        if (sheet.getBookName() != null) {
            throw new UnsupportedOperationException(NOT_SUPPORTED_FORMULA_TYPE);
        } else {
            return new Ref3DPxg(sheet, cell);
        }
    }

    @Override
    public Ptg get3DReferencePtg(AreaReference area, SheetIdentifier sheet) {
        if (sheet.getBookName() != null) {
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
