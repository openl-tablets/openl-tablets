package org.openl.rules.calc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;

/**
 * Checks the order of the steps of a united spreadsheet result, which the generated bean, its OpenAPI schema and the
 * JSON of a result follow.
 *
 * <p>The steps of the decision tables of the type come first, then those of its Spreadsheet tables, each in the order
 * of the module. A rule returning the united spreadsheet result adds its steps only once all tables of the type are
 * compiled, yet they keep the place of the rule.
 */
class UnitedSpreadsheetResultOrderTest {

    private static final String PROJECT = "test-resources/functionality/EPBDS-12236_United_steps_in_table_order";

    @Test
    void stepsOfTheRuleComeBeforeThoseOfTheSpreadsheet() throws Exception {
        var compiled = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
                .setProject(PROJECT)
                .setExecutionMode(false)
                .build()
                .getCompiledOpenClass();
        assertFalse(compiled.hasErrors(), () -> compiled.getAllMessages().toString());
        var type = (CustomSpreadsheetResultOpenClass) compiled.getOpenClass().findType("SpreadsheetResultmyTotal");

        assertArrayEquals(new String[]{"Step1", "Sum"}, type.getRowNames());
    }
}
