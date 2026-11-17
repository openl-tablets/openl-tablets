package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.HeaderSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IOpenLTable;
import org.openl.source.impl.StringSourceCodeModule;

/**
 * Verifies how the optional name of a table is read from its header and written to the header of a copy.
 *
 * @author Yury Molchan
 */
class TableHeaderNamesTest {

    @ParameterizedTest
    @CsvSource({"Conditions, XLS_CONDITIONS, true", "' Constants ', XLS_CONSTANTS, true",
            "Conditions MyConditions, XLS_CONDITIONS, false", "Test hello, XLS_TEST_METHOD, true",
            "Test hello helloChecks, XLS_TEST_METHOD, false", "Run hello, XLS_RUN_METHOD, true",
            "Properties, XLS_PROPERTIES, true", "Environment, XLS_ENVIRONMENT, true",
            "Rules, XLS_DT, false", "Notes, XLS_OTHER, false"})
    void aHeaderThatLeavesTheNameOutIsUnnamed(String header, XlsNodeTypes type, boolean unnamed) {
        assertEquals(unnamed, TableHeaderNames.isUnnamed(node(header, type)));
    }

    @ParameterizedTest
    @CsvSource({"Test hello, XLS_TEST_METHOD, Checked, Test hello Checked",
            "Test hello helloChecks, XLS_TEST_METHOD, , Test hello",
            "'Test  hello\nhelloChecks', XLS_TEST_METHOD, Checked, Test hello Checked",
            "Run hello helloRun extra, XLS_RUN_METHOD, Checked, Run hello Checked",
            "Properties Shared, XLS_PROPERTIES, , Properties",
            "Environment, XLS_ENVIRONMENT, Checked, Environment Checked",
            "Constants, XLS_CONSTANTS, Rates, Constants Rates"})
    void theCopyKeepsTheWordsBeforeTheName(String header, XlsNodeTypes type, String name, String copy) {
        assertEquals(copy, TableHeaderNames.headerOf(node(header, type), name));
    }

    /** A module of a name of more than one word is quoted in backticks, and stays one word of the header. */
    @Test
    void aQuotedNameIsOneWord() {
        var node = node("Test `My Rules`.hello", XlsNodeTypes.XLS_TEST_METHOD);

        assertEquals(true, TableHeaderNames.isUnnamed(node));
        assertEquals("Test `My Rules`.hello Checked", TableHeaderNames.headerOf(node, "Checked"));
    }

    @Test
    void aSignatureIsLeftToTheCaller() {
        assertNull(TableHeaderNames.headerOf(node("Rules String hello(String name)", XlsNodeTypes.XLS_DT), "Hi"));
    }

    @ParameterizedTest
    @CsvSource({"Test hello helloChecks, XLS_TEST_METHOD, , hello", "Properties Shared, XLS_PROPERTIES, , Properties",
            "Environment, XLS_ENVIRONMENT, , Environment", "Test hello, XLS_TEST_METHOD, Checked, Checked",
            "Rules String hello(), XLS_DT, , ''"})
    void theCopyIsShownUnderItsNameOrTheNameOfItsHeader(String header, XlsNodeTypes type, String name,
            String shown) {
        var source = mock(IOpenLTable.class);
        var node = node(header, type);
        when(source.getSyntaxNode()).thenReturn(node);

        assertEquals(shown, TableHeaderNames.shownName(source, name));
    }

    private static TableSyntaxNode node(String header, XlsNodeTypes type) {
        var node = mock(TableSyntaxNode.class);
        var headerNode = mock(HeaderSyntaxNode.class);
        when(node.getNodeType()).thenReturn(type);
        when(node.getHeader()).thenReturn(headerNode);
        when(headerNode.getModule()).thenReturn(new StringSourceCodeModule(header, null));
        return node;
    }
}
