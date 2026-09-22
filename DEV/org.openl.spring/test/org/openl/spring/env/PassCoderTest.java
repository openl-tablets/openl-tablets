package org.openl.spring.env;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/**
 * @author Pavel Tarasevich
 */

class PassCoderTest {

    private static final String CIPHER = "AES/CBC/PKCS5Padding";

    private static final String PASS = "testPass";
    private static final String KEY = "ksadbflkjsbadflk sdlfknlksajndflkjsnadf jsakidjbfl kjbsdlkfjb saljnd fs";
    private static final String WRONG_KEY = "fngnsgdlkjfngsdlk lsfng ljsdfk jndfgsljn gs";

    @Test
    void testPassCodingEncoding() {
        String codedPass = null;

        try {
            codedPass = PassCoder.encode(PASS, KEY, CIPHER);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(codedPass);

        String decodedPass = null;

        try {
            decodedPass = PassCoder.decode(codedPass, WRONG_KEY, CIPHER);
        } catch (Exception e) {
            // skip exception which wrong key
        }

        assertNull(decodedPass);

        try {
            decodedPass = PassCoder.decode(codedPass, KEY, CIPHER);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(PASS, decodedPass);
    }

    @Test
    void testEmpty() throws Exception {
        assertEquals("password", PassCoder.encode("password", "", CIPHER));
        assertEquals("password", PassCoder.encode("password", " ", CIPHER));
        assertEquals("password", PassCoder.encode("password", null, CIPHER));
        assertEquals("", PassCoder.encode("", "key", CIPHER));
        assertEquals(" ", PassCoder.encode(" ", "key", CIPHER));
        assertNull(PassCoder.encode(null, "key", CIPHER));
        assertEquals("", PassCoder.encode("", "", CIPHER));

        assertEquals("password", PassCoder.decode("password", "", CIPHER));
        assertEquals("password", PassCoder.decode("password", " ", CIPHER));
        assertEquals("password", PassCoder.decode("password", null, CIPHER));
        assertEquals("", PassCoder.decode("", "key", CIPHER));
        assertEquals(" ", PassCoder.decode(" ", "key", CIPHER));
        assertNull(PassCoder.decode(null, "key", CIPHER));
        assertEquals("", PassCoder.decode("", "", CIPHER));
    }
}
