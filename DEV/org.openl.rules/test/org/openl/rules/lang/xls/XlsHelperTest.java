package org.openl.rules.lang.xls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;

class XlsHelperTest {

    @Test
    void recognizesKnownTableHeaders() {
        assertTrue(XlsHelper.isKnownTableHeader("Datatype"));
        assertTrue(XlsHelper.isKnownTableHeader("Rules"));
        assertTrue(XlsHelper.isKnownTableHeader("Spreadsheet"));
        // only the first token decides the type
        assertTrue(XlsHelper.isKnownTableHeader("Datatype Greeting"));
        assertTrue(XlsHelper.isKnownTableHeader("Rules void greeting(int hour)"));
        // keyword aliases are recognized too
        assertTrue(XlsHelper.isKnownTableHeader("DT void rule()"));
        assertTrue(XlsHelper.isKnownTableHeader("Calc result"));
        assertTrue(XlsHelper.isKnownTableHeader("Test greeting"));
    }

    @Test
    void rejectsUnknownTableHeaders() {
        assertFalse(XlsHelper.isKnownTableHeader("Foo"));
        assertFalse(XlsHelper.isKnownTableHeader("NotATable bar"));
        assertFalse(XlsHelper.isKnownTableHeader("42"));
        assertFalse(XlsHelper.isKnownTableHeader(""));
        assertFalse(XlsHelper.isKnownTableHeader("   "));
        assertFalse(XlsHelper.isKnownTableHeader(null));
    }

    @Test
    void namesAModuleAfterItsWorkbookOrItsFolder() {
        assertEquals("Main", moduleNameOf("file:/projects/Pricing/rules/Main.xlsx"));
        // A dot in the name of a folder is no extension
        assertEquals("Rates.v2", moduleNameOf("file:/projects/Pricing/rules/Rates.v2/"));
        assertEquals("VirtualModule", moduleNameOf(VirtualSourceCodeModule.SOURCE_URI));
        // An address is read as written, with a space in it as well
        assertEquals("Main", moduleNameOf("file:/C:/My Rules/Main.xlsx"));
        assertEquals("UndefinedXlsType", moduleNameOf("rules/Main.xlsx"));
    }

    private static String moduleNameOf(String uri) {
        var source = new StringSourceCodeModule("", uri);
        return XlsHelper.getModuleName(new XlsModuleSyntaxNode(new WorkbookSyntaxNode[0], source, List.of()));
    }
}
