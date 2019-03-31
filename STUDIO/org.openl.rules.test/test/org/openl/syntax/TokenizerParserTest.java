/*
 * Created on Nov 10, 2004
 *
 * Developed by OpenRules Inc. 2003,2004
 */
package org.openl.syntax;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;

import org.junit.Assert;
import junit.framework.TestCase;

/**
 * @author snshor
 *
 */
public class TokenizerParserTest extends TestCase {

    /**
     * Constructor for TokenizerParserTest.
     *
     * @param name
     */
    public TokenizerParserTest(String name) {
        super(name);
    }

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

        System.out.println(
            "Time: " + (end - start) + " 1 run: " + 1000.0 * (end - start) / n + "mks" + " per char: " + 1000.0 * (end - start) / n / test
                .length() + "mks");

    }

    public void testTokenize() throws Exception {
        IdentifierNode[] idn = Tokenizer.tokenize(new StringSourceCodeModule("vehicle   ", null), ". \n\r");

        Assert.assertEquals("vehicle", idn[0].getIdentifier());
    }

}
