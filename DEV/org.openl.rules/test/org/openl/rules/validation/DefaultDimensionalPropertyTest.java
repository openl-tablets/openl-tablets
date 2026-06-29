package org.openl.rules.validation;

import org.junit.jupiter.api.Test;

import org.openl.rules.BaseOpenlBuilderHelper;

class DefaultDimensionalPropertyTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/validation/TestPropertyValidation.xls";

    public DefaultDimensionalPropertyTest() {
        super(SRC);
    }

    @Test
    void testError() {
        getCompiledOpenClass().getOpenClass();
    }

}
