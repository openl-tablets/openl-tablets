package org.openl.spring.env;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Pavel Tarasevich
 *
 */

public class PassCoderTest {

    private static final String CIPHER = "AES/CBC/PKCS5Padding";

    private String pass = "testPass";
    private String key = "ksadbflkjsbadflk sdlfknlksajndflkjsnadf jsakidjbfl kjbsdlkfjb saljnd fs";
    private String wrongKey = "fngnsgdlkjfngsdlk lsfng ljsdfk jndfgsljn gs";

    @Test
    public void testPassCodingEncoding() {
        String codedPass = null;

        try {
            codedPass = PassCoder.encode(pass, key, CIPHER);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(codedPass);

        String decodedPass = null;

        try {
            decodedPass = PassCoder.decode(codedPass, wrongKey, CIPHER);
        } catch (Exception e) {
            // skip exception which wrong key
        }

        assertNull(decodedPass);

        try {
            decodedPass = PassCoder.decode(codedPass, key, CIPHER);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(pass, decodedPass);
    }

    @Test
    public void testEmpty() throws Exception {
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
