package org.openl.config;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.repository.config.PassCoder;

/**
 * @author Pavel Tarasevich
 *
 */

public class PassCoderTest {
    private String pass = "testPass";
    private String key = "ksadbflkjsbadflk sdlfknlksajndflkjsnadf jsakidjbfl kjbsdlkfjb saljnd fs";
    private String wrongKey = "fngnsgdlkjfngsdlk lsfng ljsdfk jndfgsljn gs";

    @Test
    public void testPassCodingEncoding() {
        String codedPass = null;

        try {
            codedPass = PassCoder.encode(pass, key);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(codedPass);

        String decodedPass = null;

        try {
            decodedPass = PassCoder.decode(codedPass, wrongKey);
        } catch (Exception e) {
            // skip exception which wrong key
        }

        assertNull(decodedPass);

        try {
            decodedPass = PassCoder.decode(codedPass, key);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(pass, decodedPass);
    }

    @Test
    public void testEmpty() throws Exception {
        assertEquals("password", PassCoder.encode("password", ""));
        assertEquals("password", PassCoder.encode("password", " "));
        assertEquals("password", PassCoder.encode("password", null));
        assertEquals("", PassCoder.encode("", "key"));
        assertEquals("", PassCoder.encode(" ", "key"));
        assertEquals("", PassCoder.encode(null, "key"));
        assertEquals("", PassCoder.encode("", ""));

        assertEquals("password", PassCoder.decode("password", ""));
        assertEquals("password", PassCoder.decode("password", " "));
        assertEquals("password", PassCoder.decode("password", null));
        assertEquals("", PassCoder.decode("", "key"));
        assertEquals("", PassCoder.decode(" ", "key"));
        assertEquals("", PassCoder.decode(null, "key"));
        assertEquals("", PassCoder.decode("", ""));
    }
}
