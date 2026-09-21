package org.openl.syntax.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import org.openl.source.impl.StringSourceCodeModule;
import org.openl.util.text.LocationUtils;

class TokenizerParserTest {

    @Test
    void testFirstToken1() {
        var testValue = "Rules double hello (int param1, String param2)";
        var source = new StringSourceCodeModule(testValue, null);
        try {
            assertEquals("Rules", Tokenizer.firstToken(source, " \n\r").getIdentifier());
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }

    @Test
    void testFirstToken2() {
        var testValue = "   Rules double hello (int param1, String param2)";
        var source = new StringSourceCodeModule(testValue, null);
        try {
            assertEquals("Rules", Tokenizer.firstToken(source, " \n\r").getIdentifier());
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }

    @Test
    void stopsSkippingWhenTheLocationStartsBeyondTheEndOfTheSource() throws Exception {
        var source = new StringSourceCodeModule("ab", null);

        var tokens = Tokenizer.tokenize(source, " \n\r", LocationUtils.createTextInterval(10, 12));

        assertEquals(0, tokens.length);
    }
}
