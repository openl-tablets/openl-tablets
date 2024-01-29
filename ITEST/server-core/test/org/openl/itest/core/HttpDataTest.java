package org.openl.itest.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class HttpDataTest {

    @Test
    public void testReadFile() throws Exception {
        HttpData chuncked = HttpData.readFile("/chuncked.resp");
        HttpData clHeader = HttpData.readFile("/content-length.resp");
        HttpData undefined = HttpData.readFile("/undefined-length.resp");

        chuncked.assertTo(undefined);
        clHeader.assertTo(undefined);
        undefined.assertTo(undefined);

        try {
            undefined.assertTo(clHeader);
            fail("Non reachable");
        } catch (AssertionError er) {
            assertEquals("Content-Length ==> expected: <14> but was: <null>", er.getMessage());
        }

        try {
            undefined.assertTo(chuncked);
            fail("Non reachable");
        } catch (AssertionError er) {
            assertEquals("Transfer-Encoding ==> expected: <chunked> but was: <null>", er.getMessage());
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
            assertEquals("Unexpected end of the stream. Expected CRLF in the end of the line.", er.getMessage());
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

    @Test
    public void testResponse204() throws Exception {
        final HttpData fullNoContent = HttpData.readFile("/no-content-full.resp");
        final HttpData shortNoContent = HttpData.readFile("/no-content-short.resp");
        final HttpData noContentWithBody = HttpData.readFile("/no-content-non-standard.resp");
        fullNoContent.assertTo(shortNoContent);
        fullNoContent.assertTo(noContentWithBody);
        shortNoContent.assertTo(noContentWithBody);
    }
}
