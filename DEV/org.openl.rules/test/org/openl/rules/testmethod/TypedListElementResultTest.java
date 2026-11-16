package org.openl.rules.testmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

/**
 * A Test table checks the fields of a List element whose type the column declares, like
 * {@code _res_.myList[1]: Item.field1}, or the tested table collects, like {@code _res_[0].field1}. The element of a
 * collected map is read by a key of the collected key type.
 *
 * @author Yury Molchan
 */
class TypedListElementResultTest {

    private static final String FILE_NAME = "test/rules/testmethod/EPBDS-8717_Typed_List_Element_Result.xlsx";

    @Test
    void checksTheFieldsOfTypedListElements() {
        var instance = TestUtils.create(FILE_NAME, ITest.class);

        var passed = instance.echoPass();
        assertEquals(0, passed.getNumberOfFailures());
        assertEquals(2, passed.getTestResultColumnDisplayNames().length);

        var failed = instance.echoFail();
        assertEquals(1, failed.getNumberOfFailures());
        assertEquals(1, failed.getNumberOfAssertionFailures());
    }

    @Test
    void checksTheElementsOfCollectedLists() {
        var instance = TestUtils.create(FILE_NAME, ITest.class);

        var passed = instance.collectedPass();
        assertEquals(0, passed.getNumberOfFailures());
        assertEquals(2, passed.getTestResultColumnDisplayNames().length);

        var failed = instance.collectedFail();
        assertEquals(1, failed.getNumberOfFailures());
    }

    @Test
    void checksTheElementsOfCollectedMapsByTheirKeyType() {
        var instance = TestUtils.create(FILE_NAME, ITest.class);

        var passed = instance.keyedPass();
        assertEquals(0, passed.getNumberOfFailures());
        assertEquals(2, passed.getTestResultColumnDisplayNames().length);

        var failed = instance.keyedFail();
        assertEquals(1, failed.getNumberOfFailures());
    }

    public interface ITest {
        TestUnitsResults keyedPass();

        TestUnitsResults keyedFail();

        TestUnitsResults echoPass();

        TestUnitsResults echoFail();

        TestUnitsResults collectedPass();

        TestUnitsResults collectedFail();
    }
}
