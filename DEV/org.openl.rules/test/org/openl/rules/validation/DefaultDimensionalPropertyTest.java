package org.openl.rules.validation;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import org.openl.rules.BaseOpenlBuilderHelper;

class DefaultDimensionalPropertyTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/validation/TestPropertyValidation.xls";

    public DefaultDimensionalPropertyTest() {
        super(SRC);
    }

    @Test
    void testError() {
        var compiledOpenClass = getCompiledOpenClass();

        assertFalse(compiledOpenClass.hasErrors(), () -> compiledOpenClass.getAllMessages().toString());
        assertNotNull(compiledOpenClass.getOpenClass());
    }

}
