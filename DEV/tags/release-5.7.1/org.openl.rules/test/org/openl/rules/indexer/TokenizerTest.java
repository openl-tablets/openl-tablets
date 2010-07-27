package org.openl.rules.indexer;

import static org.junit.Assert.*;

import org.junit.Test;

public class TokenizerTest {
    
    @Test
    public void testParse() {        
        String[] resStr = Tokenizer.parse("driver");
        assertEquals("driver", resStr[0]);                
        for(String str : resStr) {
            System.out.println(str);
        }
    }
    
    @Test
    public void testParse1() {        
        String[] resStr1 = Tokenizer.parse("dr_ive#me&yo");
        assertEquals("dr_ive#me&yo", resStr1[0]);
        
        for(String str : resStr1) {
            System.out.println(str);
        }
        
    }
    
    @Test
    public void testParse2() {
        String[] resStr2 = Tokenizer.parse("dr_ive#me&yo and #find");
        assertEquals("dr_ive#me&yo", resStr2[0]);
        assertEquals("and", resStr2[1]);
        assertEquals("find", resStr2[2]);
        
        for(String str : resStr2) {
            System.out.println(str);
        }
    }
    
    @Test
    public void testParse3() {
        String[] resStr2 = Tokenizer.parse("dr_ive# 4.0 #find3.75 %84 999.5%");
        assertEquals("dr_ive#", resStr2[0]);
        assertEquals("4.0", resStr2[1]);
        assertEquals("find3", resStr2[2]);
        assertEquals("75", resStr2[3]);
        assertEquals("84", resStr2[4]);
        assertEquals("999.5%", resStr2[5]);
        
        for(String str : resStr2) {
            System.out.println(str);
        }
    }
    
    @Test
    public void testParse4() {
        String[] resStr2 = Tokenizer.parse("Driver-Premium");
        for(String str : resStr2) {
            System.out.println(str);
        }
        assertTrue(3 == resStr2.length);
        assertEquals("Driver-Premium", resStr2[0]);
        assertEquals("Driver", resStr2[1]);
        assertEquals("Premium", resStr2[2]);
    }
    
    @Test
    public void testParse5() {
        String[] resStr2 = Tokenizer.parse("Driver-Premium-Forever");
        for(String str : resStr2) {
            System.out.println(str);
        }
        assertTrue(4 == resStr2.length);
        assertEquals("Driver-Premium-Forever", resStr2[0]);
        assertEquals("Driver", resStr2[1]);
        assertEquals("Premium", resStr2[2]);
        assertEquals("Forever", resStr2[3]);
    }

}
