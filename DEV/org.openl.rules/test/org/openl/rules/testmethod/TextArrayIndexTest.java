package org.openl.rules.testmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.types.IOpenClass;

/**
 * Checks a test table whose expected column takes an element of an array by a text, {@code items["0"]}.
 *
 * @author Yury Molchan
 */
class TextArrayIndexTest extends BaseOpenlBuilderHelper {

    TextArrayIndexTest() {
        super("test/rules/testmethod/TextArrayIndex.xlsx");
    }

    /** The column is reported once, when the table is compiled; reading the cases of the table fails on nothing. */
    @Test
    void readsTheCasesOfATableWhoseIndexIsNotANumber() {
        var messages = getCompiledOpenClass().getAllMessages();
        assertEquals(1, messages.size());
        assertEquals("Failed to parse an array index.", messages.iterator().next().getSummary());

        var suite = assertInstanceOf(TestSuiteMethod.class,
                getCompiledOpenClass().getOpenClassWithErrors().getMethod("orderTest", IOpenClass.EMPTY));
        assertEquals(1, suite.getTests().length);
    }
}
