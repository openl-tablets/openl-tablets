package org.openl.rules.indexer;

import static org.junit.Assert.*;

import org.junit.Test;

public class IndexQueryParserTest {
    
    @Test
    public void testParsing() {        
        IndexQuery iq = IndexQueryParser.parse("Driver Licence");
        assertTrue(2 == iq.getTokensInclude().length);
        assertEquals("Driver", iq.getTokensInclude()[0][0]);        
        assertEquals("Licence", iq.getTokensInclude()[1][0]);        
        assertTrue(0 == iq.tokensExclude.length);
    }
    
    @Test
    public void testParsing1() {
        IndexQuery iq = IndexQueryParser.parse("\"Child-root\"");
        assertTrue(0 == iq.getTokensInclude().length);
        assertTrue(1 == iq.tokensExclude.length);
        assertEquals("Child-root", iq.tokensExclude[0][0]);        
    }
    
    @Test
    public void testParsing2() {
        IndexQuery iq = IndexQueryParser.parse("\"Child-root");
        assertTrue(0 == iq.getTokensInclude().length);
        assertTrue(1 == iq.tokensExclude.length);
        assertEquals("Child-root", iq.tokensExclude[0][0]);        
    }
    
    @Test
    public void testParsing3() {
        IndexQuery iq = IndexQueryParser.parse("\"child\"");
        assertTrue(1 == iq.getTokensInclude().length);
        assertTrue(0 == iq.tokensExclude.length);
        assertEquals("child", iq.getTokensInclude()[0][0]);        
    }
    
    @Test
    public void testParsing4() {
        IndexQuery iq = IndexQueryParser.parse("chi\"ca-go");
        assertTrue(1 == iq.getTokensInclude().length);
        assertTrue(1 == iq.tokensExclude.length);
        assertEquals("chi", iq.getTokensInclude()[0][0]);
        assertEquals("ca-go", iq.tokensExclude[0][0]);
    }
    

}
