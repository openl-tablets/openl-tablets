package org.openl.itest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

public class HttpDataTest {

    @Test
    public void testReadFile() throws IOException {
        HttpData chuncked = HttpData.readFile("/chuncked.resp");
        HttpData clHeader = HttpData.readFile("/content-length.resp");
        HttpData undefined = HttpData.readFile("/undefined-length.resp");

        chuncked.assertTo(undefined);
        clHeader.assertTo(undefined);
        undefined.assertTo(undefined);

        try {
            undefined.assertTo(clHeader);
            fail("Non reachable");
        } catch (RuntimeException er) {
            assertEquals("java.lang.AssertionError: Content-Length expected:<14> but was:<null>", er.getMessage());
        }

        try {
            undefined.assertTo(chuncked);
            fail("Non reachable");
        } catch (RuntimeException er) {
            assertEquals("java.lang.AssertionError: Transfer-Encoding expected:<chunked> but was:<null>",
                er.getMessage());
        }
    }

    @Test
    public void testReadAbsentFile() throws IOException {
        HttpData absent = HttpData.readFile("/absent");
        assertNull(absent);
    }

    @Test
    public void testWrongLength() {
        try {
            HttpData.readFile("/wrong-length.resp");
            fail("Non reachable");
        } catch (IOException er) {
            assertEquals("Unexpected size of the body.", er.getMessage());
        }
    }

    @Test
    public void testWrongHeader() {
        try {
            HttpData.readFile("/wrong-header.resp");
            fail("Non reachable");
        } catch (IOException er) {
            assertEquals("Unexpected end of the stream.", er.getMessage());
        }
    }

    @Test
    public void testWrongChuncked() {
        try {
            HttpData.readFile("/wrong-chuncked.resp");
            fail("Non reachable");
        } catch (IOException er) {
            assertEquals("Unexpected format of the chunk.", er.getMessage());
        }

        try {
            HttpData.readFile("/wrong-chuncked2.resp");
            fail("Non reachable");
        } catch (IOException er) {
            assertEquals("Unexpected size of the chunk.", er.getMessage());
        }
    }
}
