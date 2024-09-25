package org.openl.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.openl.CompiledOpenClass;
import org.openl.rules.runtime.RulesEngineFactory;

class LiteralCompilationTest {

    @Test
    void testLiteralError() {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<>(
                "./test-resources/IncorrectVocabulary/EPBDS-14557.xlsx");
        engineFactory.setExecutionMode(true);
        CompiledOpenClass compiledOpenClass = engineFactory.getCompiledOpenClass();

        assertTrue(compiledOpenClass.hasErrors(), "Expected an error in the project");
        assertEquals(1, compiledOpenClass.getAllMessages().size(), "Must have only one error");
        assertEquals("Object 'F' is outside of valid domain 'Gender'. Valid values: [Male, Female]",
                compiledOpenClass.getAllMessages().iterator().next().getSummary(),
                "Incorrect error message");
    }
}
