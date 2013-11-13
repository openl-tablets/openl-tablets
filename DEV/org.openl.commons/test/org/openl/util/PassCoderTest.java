package org.openl.util;

import static org.junit.Assert.*;

import org.junit.Test;

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
}
