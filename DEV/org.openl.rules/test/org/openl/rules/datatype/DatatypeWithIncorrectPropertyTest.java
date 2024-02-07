package org.openl.rules.datatype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.openl.CompiledOpenClass;
import org.openl.rules.BaseOpenlBuilderHelper;

public class DatatypeWithIncorrectPropertyTest extends BaseOpenlBuilderHelper {
    public DatatypeWithIncorrectPropertyTest() {
        super("test/rules/datatype/DatatypeWithIncorrectProperty.xlsx");
    }

    @Test
    public void testHaveOnlyOneError() {
        CompiledOpenClass compiledOpenClass = getCompiledOpenClass();

        assertTrue(compiledOpenClass.hasErrors(), "Expected an error in the project");
        assertEquals(1, compiledOpenClass.getAllMessages().size(), "Datatype must have only one error");
        assertEquals("Property 'precision' cannot be defined in 'Datatype' table.",
                compiledOpenClass.getAllMessages().iterator().next().getSummary(),
                "Incorrect error message");
    }
}
