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
        assertTrue(1 == iq.getTokensInclude().length);
        assertTrue(0 == iq.tokensExclude.length);
        assertEquals("Child-root", iq.getTokensInclude()[0][0]);        
    }
    
    @Test
    public void testParsing2() {
        IndexQuery iq = IndexQueryParser.parse("\"Child-root");
        assertTrue(1 == iq.getTokensInclude().length);
        assertTrue(0 == iq.tokensExclude.length);
        assertEquals("Child-root", iq.getTokensInclude()[0][0]);        
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
        assertTrue(2 == iq.getTokensInclude().length);
        assertTrue(0 == iq.tokensExclude.length);
        assertEquals("chi", iq.getTokensInclude()[0][0]);
        assertEquals("ca-go", iq.getTokensInclude()[1][0]);
    }
    
    @Test
    public void testParsing5() {
        IndexQuery iq = IndexQueryParser.parse("Young Driver");        
        assertTrue(2 == iq.getTokensInclude().length);
        assertTrue(0 == iq.tokensExclude.length);
        assertEquals("Young", iq.getTokensInclude()[0][0]);
        assertEquals("Driver", iq.getTokensInclude()[1][0]);
    }
    
    @Test
    public void testParsing6() {
        IndexQuery iq = IndexQueryParser.parse("\"openl tablets\"");        
        assertTrue(1 == iq.getTokensInclude().length);
        assertTrue(0 == iq.tokensExclude.length);
        assertEquals("openl", iq.getTokensInclude()[0][0]);
        assertEquals("tablets", iq.getTokensInclude()[0][1]);
    }
    
    @Test
    public void testParsing7() {
        IndexQuery iq = IndexQueryParser.parse("Child-root");
        assertTrue(1 == iq.getTokensInclude().length);
        assertTrue(0 == iq.tokensExclude.length);
        assertEquals("Child-root", iq.getTokensInclude()[0][0]);        
    }
    
    @Test
    public void testParsing8() {
        IndexQuery iq = IndexQueryParser.parse("\"High Risk\"");        
        assertTrue(1 == iq.getTokensInclude().length);
        assertTrue(0 == iq.tokensExclude.length);
        assertEquals("High", iq.getTokensInclude()[0][0]);
        assertEquals("Risk", iq.getTokensInclude()[0][1]);
    }
    
    @Test
    public void testParsing9() {
        IndexQuery iq = IndexQueryParser.parse("High Risk");        
        assertTrue(2 == iq.getTokensInclude().length);
        assertTrue(0 == iq.tokensExclude.length);
        assertEquals("High", iq.getTokensInclude()[0][0]);
        assertEquals("Risk", iq.getTokensInclude()[1][0]);
    }
    

}
