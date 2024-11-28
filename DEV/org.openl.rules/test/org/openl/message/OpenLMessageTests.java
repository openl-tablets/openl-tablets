package org.openl.message;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.openl.rules.BaseOpenlBuilderHelper;

public class OpenLMessageTests extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/OpenLMessagesTest.xlsx";

    public OpenLMessageTests() {
        super(SRC);
    }

    @Test
    public void testMessages() {
        for (OpenLMessage message : getCompiledOpenClass().getAllMessages()) {
            if (message instanceof OpenLErrorMessage) {
                assertTrue(message.getSourceLocation()
                        .endsWith("OpenLMessagesTest.xlsx?sheet=VehiclePremium&cell=C3&start=0&end=0"));
            }
        }

    }
}
