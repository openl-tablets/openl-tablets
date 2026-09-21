package org.openl.util.formatters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Locale;

import org.junit.jupiter.api.Test;

/** What text a number formatter reads a number out of, and what text it refuses. */
class NumberFormatterTest {

    private final NumberFormatter formatter = new NumberFormatter(Locale.US);

    @Test
    void readsTheNumberTheWholeTextStandsFor() {
        assertEquals(1L, formatter.parse("1"));
        assertEquals(0.3, formatter.parse("0.3"));
        assertEquals(-5000L, formatter.parse("-5000"));
    }

    @Test
    void refusesTextThatOnlyBeginsWithANumber() {
        // The parser can read a 1 out of this and stop; answering with it would put a value in the author's
        // place, and a cell they wrote as text would read back as the number 1.
        assertNull(formatter.parse("1abc"));
        assertNull(formatter.parse("0.3 per cent"));
    }

    @Test
    void refusesTextThatIsNoNumberAtAll() {
        assertNull(formatter.parse("abc"));
        assertNull(formatter.parse(""));
        assertNull(formatter.parse(null));
    }

    @Test
    void readsANumberWrittenWithSpaceAroundIt() {
        // The space is not part of the number, and a cell padded by its author still holds one.
        assertEquals(1L, formatter.parse("  1 "));
    }
}
