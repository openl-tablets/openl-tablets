package org.openl.message;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;

public class OpenLMessageTests extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/OpenLMessagesTest.xlsx";

    public OpenLMessageTests() {
        super(SRC);
    }

    @Test
    public void testMessages() {
        for (OpenLMessage message : getCompiledOpenClass().getMessages()) {
            if (message instanceof OpenLErrorMessage) {
                Assert.assertTrue(message.getSourceLocation()
                    .endsWith("OpenLMessagesTest.xlsx?sheet=VehiclePremium&cell=C3&start=0&end=0"));
            }
        }

    }
}
