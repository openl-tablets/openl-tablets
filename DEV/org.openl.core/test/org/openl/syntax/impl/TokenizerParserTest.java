package org.openl.syntax.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;

public class TokenizerParserTest {

    @Test
    public void testFirstToken1() {
        String testValue = "Rules double hello (int param1, String param2)";
        IOpenSourceCodeModule source = new StringSourceCodeModule(testValue, null);
        try {
            assertEquals("Rules", Tokenizer.firstToken(source, " \n\r").getIdentifier());
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testFirstToken2() {
        String testValue = "   Rules double hello (int param1, String param2)";
        IOpenSourceCodeModule source = new StringSourceCodeModule(testValue, null);
        try {
            assertEquals("Rules", Tokenizer.firstToken(source, " \n\r").getIdentifier());
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }
}
