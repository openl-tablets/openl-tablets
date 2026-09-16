package org.openl.util.formatters;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** What text a date formatter reads a date out of, and what text it refuses. */
class DateFormatterTest {

    private final DateFormatter formatter = new DateFormatter("MM/dd/yyyy");

    @Test
    void readsTheDateTheWholeTextStandsFor() {
        assertNotNull(formatter.parse("12/31/2024"));
        // The space around it is not part of the date.
        assertNotNull(formatter.parse(" 12/31/2024 "));
    }

    @Test
    void refusesTextThatOnlyBeginsWithADate() {
        // The parser can read a date out of this and stop; the date it managed is not the one the author
        // wrote, and answering with it would put a value in their place.
        assertNull(formatter.parse("12/31/2024 and later"));
    }

    @Test
    void refusesTextThatIsNoDateAtAll() {
        assertNull(formatter.parse("not a date"));
        assertNull(formatter.parse(null));
    }
}
