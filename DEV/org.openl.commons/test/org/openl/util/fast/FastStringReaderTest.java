package org.openl.util.fast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class FastStringReaderTest {

    @Test
    void skipsForwardAndStopsAtTheEnd() throws IOException {
        try (var reader = new FastStringReader("abcdef")) {
            assertEquals(2, reader.skip(2));
            assertEquals('c', reader.read());

            // Asked for more than is left, the reader skips only what is there.
            assertEquals(3, reader.skip(100));
            assertEquals(-1, reader.read());

            assertEquals(0, reader.skip(1));
        }
    }
}
