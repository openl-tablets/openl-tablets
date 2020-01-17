package org.openl.rules.data;

import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.rules.dt.DTTest;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.syntax.exception.SyntaxNodeException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DataTableCompilationErrorsLocationTest {

    private static final String FILE_NAME = "test/rules/data/EPBDS_9098_Data_table_validation.xlsx";

    private CompiledOpenClass compiledOpenClass;

    @Before
    public void setUp() {
        RulesEngineFactory<DTTest> engineFactory = new RulesEngineFactory<>(FILE_NAME);
        engineFactory.setExecutionMode(false);
        compiledOpenClass = engineFactory.getCompiledOpenClass();
    }

    @Test
    public void doTest() {
        assertTrue(compiledOpenClass.hasErrors());
        SyntaxNodeException[] bindingError = compiledOpenClass.getBindingErrors();
        assertNotNull(bindingError);
        assertEquals(5, bindingError.length);
        IDataBase dataBase = ((XlsModuleOpenClass) compiledOpenClass.getOpenClassWithErrors()).getDataBase();

        assertCompilationErrors(dataBase.getTable("EmptyKeyData"), "Empty key in an unique index.");
        assertCompilationErrors(dataBase.getTable("DuplicateKeyData"), "Duplicated key in an unique index: P0001");
        assertCompilationErrors(dataBase.getTable("DuplicateKeyDataTest"),
            "Foreign table 'DuplicateKeyData' has errors.");
        assertCompilationErrors(dataBase.getTable("EmptyKeyDataTest"), "Foreign table 'EmptyKeyData' has errors.");
        assertCompilationErrors(dataBase.getTable("UnknownKeyTest"),
            "Index Key 'P0005' is not found in the foreign table 'PolicyData'.");
        assertCompilationErrors(dataBase.getTable("PolicyData"));
    }

    protected void assertCompilationErrors(ITable table, String... expectedErrorMsgs) {
        assertNotNull(table);
        SyntaxNodeException[] errors = table.getTableSyntaxNode().getErrors();
        assertEquals(expectedErrorMsgs.length, errors.length);
        for (String expectedMessage : expectedErrorMsgs) {
            boolean matched = Stream.of(errors).map(Throwable::getMessage).anyMatch(expectedMessage::equals);
            if (!matched) {
                fail(String.format("Unable to find error message '%s' inside '%s' table syntax node!",
                    expectedMessage,
                    table.getName()));
            }
        }
    }

}
