package org.openl.rules.data;

import org.junit.Test;
import org.openl.message.OpenLMessage;
import org.openl.rules.TestUtils;

public class DataTableIncorrectTypeInCollectionTest {

    @Test
    public void testCollectionErrorMessages() {
        OpenLMessage[] errorMessages = TestUtils.collectErrorMessagesFromFileProcessing("test/rules/data/DataWithMapWithUndefinedClass.xlsx");
        TestUtils.assertErrorMessagesArePresent(errorMessages,
                "Cannot bind node: 'token=[\"address\"]",
                "Cannot find type: 'address'",
                "Cannot bind node: 'token=[\"zip\"]:integer",
                "Cannot find type: 'integer'");
        TestUtils.assertErrorMessagesAreAbsent(errorMessages,
                "The element is null");

    }
}
