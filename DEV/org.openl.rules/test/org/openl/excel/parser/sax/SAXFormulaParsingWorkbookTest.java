package org.openl.excel.parser.sax;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaRenderer;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.SharedFormula;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.junit.jupiter.api.Test;

class SAXFormulaParsingWorkbookTest {

    @Test
    void parsesUnknownFunctionWithoutThrowing() {
        // RANDBETWEEN is an Analysis ToolPak function and is not in POI's built-in function registry.
        // Before the fix, SAXFormulaParsingWorkbook.getName / getNameXPtg threw UnsupportedOperationException
        // when POI's FormulaParser tried to resolve such names as user-defined function fallbacks.
        SAXFormulaParsingWorkbook workbook = new SAXFormulaParsingWorkbook();

        Ptg[] tokens = assertDoesNotThrow(() -> FormulaParser
                .parse("RANDBETWEEN(50,1000)*10", workbook, FormulaType.CELL, 0, 0));

        // Round-trip the formula via SharedFormula conversion (zero offset) and the renderer to make sure
        // the unknown function name is preserved end-to-end — this matches the StyleIndexHandler flow.
        Ptg[] shifted = new SharedFormula(SpreadsheetVersion.EXCEL2007).convertSharedFormulas(tokens, 0, 0);
        assertEquals("RANDBETWEEN(50,1000)*10", FormulaRenderer.toFormulaString(null, shifted));
    }
}
