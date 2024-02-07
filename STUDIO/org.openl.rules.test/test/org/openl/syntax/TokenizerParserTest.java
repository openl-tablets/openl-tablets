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

import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;

/**
 * @author snshor
 */
public class TokenizerParserTest {

    private final Logger log = LoggerFactory.getLogger(TokenizerParserTest.class);

    @Test
    public void testPerformance() throws Exception {
        long start = System.currentTimeMillis();
        String test = "a123344 b1233468474 c238746374";
        int n = 1000000;
        // String delim = ". \n\r{}[]!@#$%^&*()-_+=,.<>/?;:'\"\\|";
        IOpenSourceCodeModule src = new StringSourceCodeModule(test, null);
        // TokenizerParser tp = new TokenizerParser(delim);
        for (int i = 0; i < n; ++i) {
            Tokenizer.tokenize(src, " \n\r");
            // tp.parse(new StringSourceCodeModule(test, null));
        }
        long end = System.currentTimeMillis();

        log.info("Time: {} 1 run: {}mks per char: {}mks", (end - start), 1000.0 * (end - start) / n, 1000.0 * (end - start) / n / test
                .length());

    }

    @Test
    public void testTokenize() throws Exception {
        IdentifierNode[] idn = Tokenizer.tokenize(new StringSourceCodeModule("vehicle   ", null), ". \n\r");

        assertEquals("vehicle", idn[0].getIdentifier());
    }

}
