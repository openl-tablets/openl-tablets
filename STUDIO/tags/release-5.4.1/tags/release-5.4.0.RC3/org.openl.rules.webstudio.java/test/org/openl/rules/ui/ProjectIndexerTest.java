package org.openl.rules.ui;

import static org.junit.Assert.*;

import org.junit.Test;

public class ProjectIndexerTest {
    
    private String projectHome = "test/rules/project.to.test.projectindexer";
    
    @Test 
    public void test1() {                
        ProjectIndexer indexer = new ProjectIndexer(projectHome);        
        String[][] result = indexer.getResultsForQuery("Gen", 200, null);
        assertTrue(18 == result.length);
        
        String[][] result1 = indexer.getResultsForIndex("Gender");        
        assertTrue(17 == result1.length);
        
        String[][] result2 = indexer.getResultsForQuery("\"openl tablets\"", 200, null);                
        assertTrue(1 == result2.length);
        
        String[][] result3 = indexer.getResultsForQuery("\"ope tabl\"", 200, null);                
        assertTrue("Must be", 1 == result3.length);
        
        String[][] result4 = indexer.getResultsForQuery("\"openl tabl\"", 200, null);                
        assertTrue("Must be", 1 == result4.length);
        
        String[][] result5 = indexer.getResultsForQuery("\"tablets\"", 200, null);                
        assertTrue(1 == result5.length);
        
        String[][] result6 = indexer.getResultsForQuery("tablets", 200, null);                
        assertTrue(1 == result6.length);
        
        String[][] result7 = indexer.getResultsForQuery("openl tabl", 200, null);                
        assertTrue(13 == result7.length);
        
        String[][] result8 = indexer.getResultsForQuery("Driver-data", 200, null);                
        assertTrue(1 == result8.length);
        
        String[][] result9 = indexer.getResultsForQuery("\"dri pr\"", 200, null);                
        assertTrue(36 == result9.length);
    }
    
}
