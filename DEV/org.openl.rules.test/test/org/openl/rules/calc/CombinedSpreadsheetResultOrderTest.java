package org.openl.rules.calc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.types.IOpenClass;

/**
 * Checks the order of the steps of a type combining several spreadsheet results, which the generated bean, its OpenAPI
 * schema and the JSON of a result follow.
 *
 * <p>The combined type takes the steps of the spreadsheets in the order of their names, at every start. So does the
 * copy of the type in a module that depends on the module of the spreadsheets.
 */
class CombinedSpreadsheetResultOrderTest {

    private static final String PROJECT = "test-resources/functionality/EPBDS-17330_Combined_type_of_dependency";

    @Test
    void stepsComeInTheOrderOfTheNamesOfTheSpreadsheets() throws Exception {
        var compiled = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
                .setProject(PROJECT)
                .setExecutionMode(false)
                .build()
                .getCompiledOpenClass();
        assertFalse(compiled.hasErrors(), () -> compiled.getAllMessages().toString());
        var module = compiled.getOpenClass();
        var ofCore = module.findType("SpreadsheetResultCore").getField("$Other").getType();
        var ofPlan = module.findType("SpreadsheetResultPlan").getField("$Core").getType().getField("$Other").getType();

        var expected = new String[]{"Other", "BuyUpRate", "CoreRate"};
        assertArrayEquals(expected, rowNames(ofCore));
        assertArrayEquals(expected, rowNames(ofPlan));
    }

    private static String[] rowNames(IOpenClass type) {
        return ((CustomSpreadsheetResultOpenClass) type).getRowNames();
    }
}
