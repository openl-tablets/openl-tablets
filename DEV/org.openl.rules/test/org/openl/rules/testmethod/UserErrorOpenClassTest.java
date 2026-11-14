package org.openl.rules.testmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.vm.SimpleVM;

/**
 * @author Yury Molchan
 */
class UserErrorOpenClassTest {

    private final UserErrorOpenClass errorClass = new UserErrorOpenClass();

    private Object read(Object error, String field) {
        return errorClass.getField(field).get(error, new SimpleVM().getRuntimeEnv());
    }

    @Test
    void readsGetterOfError() {
        assertEquals("ERR-1", read(new Problem("ERR-1", true), "code"));
        assertNull(read(new Problem(null, false), "code"));
        assertEquals(true, read(new Problem("ERR-1", true), "retryable"));
    }

    @Test
    void readsComponentOfRecord() {
        assertEquals("ERR-2", read(new Failure("ERR-2"), "code"));
    }

    @Test
    void readsStepOfSpreadsheet() {
        var spreadsheet = new SpreadsheetResult(new Object[][]{{"ERR-1"}},
                new String[]{"Code"},
                new String[]{"Value"},
                new String[]{"Code"},
                new String[]{"Value"},
                null);

        assertEquals("ERR-1", read(spreadsheet, "$Code"));
        var e = assertThrows(OpenLRuntimeException.class, () -> read(spreadsheet, "$Missing"));
        assertEquals("Field '$Missing' is not found in the error of type 'SpreadsheetResult'.", e.getMessage());
    }

    @Test
    void failsOnFieldErrorDoesNotHave() {
        var e = assertThrows(OpenLRuntimeException.class, () -> read(new String[]{"aaa", "bbb"}, "BlaBla"));
        assertEquals("Field 'BlaBla' is not found in the error of type 'String[]'.", e.getMessage());
    }

    @Test
    void emptyErrorHasNoFields() {
        assertNull(read(null, "BlaBla"));
    }

    public static final class Problem {
        private final String code;
        private final boolean retryable;

        Problem(String code, boolean retryable) {
            this.code = code;
            this.retryable = retryable;
        }

        public String getCode() {
            return code;
        }

        public boolean isRetryable() {
            return retryable;
        }
    }

    public record Failure(String code) {
    }
}
