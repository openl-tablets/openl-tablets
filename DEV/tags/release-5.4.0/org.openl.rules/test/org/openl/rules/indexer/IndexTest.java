package org.openl.rules.indexer;

import static org.junit.Assert.*;

import org.junit.Test;

public class IndexTest {
    
    @Test
    public void testGetRoot() {
        String token = "drivers";
        String result = Index.getRoot(token);
        assertEquals("driver", result);
        
        String token1 = "";
        String result1 = Index.getRoot(token1);
        assertEquals("", result1);
        
    }

}
