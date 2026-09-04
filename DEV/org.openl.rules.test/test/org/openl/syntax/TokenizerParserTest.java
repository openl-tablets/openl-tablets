/*
 * Created on Nov 10, 2004
 *
 * Developed by OpenRules Inc. 2003,2004
 */
package org.openl.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;

/**
 * @author snshor
 */
class TokenizerParserTest {

    private final Logger log = LoggerFactory.getLogger(TokenizerParserTest.class);

    @Test
    void testPerformance() throws Exception {
        var start = System.currentTimeMillis();
        var test = "a123344 b1233468474 c238746374";
        var n = 1000000;
        // String delim = ". \n\r{}[]!@#$%^&*()-_+=,.<>/?;:'\"\\|";
        var src = new StringSourceCodeModule(test, null);
        // TokenizerParser tp = new TokenizerParser(delim);
        for (var i = 0; i < n; ++i) {
            Tokenizer.tokenize(src, " \n\r");
            // tp.parse(new StringSourceCodeModule(test, null));
        }
        var end = System.currentTimeMillis();

        log.info("Time: {} 1 run: {}mks per char: {}mks", (end - start), 1000.0 * (end - start) / n, 1000.0 * (end - start) / n / test
                .length());

    }

    @Test
    void testTokenize() throws Exception {
        IdentifierNode[] idn = Tokenizer.tokenize(new StringSourceCodeModule("vehicle   ", null), ". \n\r");

        assertEquals("vehicle", idn[0].getIdentifier());
    }

}
