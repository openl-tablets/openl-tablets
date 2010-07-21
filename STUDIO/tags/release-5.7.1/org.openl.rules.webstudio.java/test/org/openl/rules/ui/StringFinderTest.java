package org.openl.rules.ui;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.tableeditor.model.ui.util.StringHighlighter;

public class StringFinderTest {
    
    @Test
    public void testStringFinder() {
        String[] tokens = new String[]{"driver"};
        String src = "go away driver of my dream";
        StringHighlighter sf = new StringHighlighter(tokens, src);
        String result = sf.highlightStringsInText();
        System.out.println("result="+result);
        assertEquals("go away <b>driver</b> of my dream", result);
    }
    
    @Test
    public void testStringFinder1() {
        String[] tokens = new String[]{"driver"};
        String src = "go away driver of my dream and her driver is cooler";
        StringHighlighter sf = new StringHighlighter(tokens, src);
        String result = sf.highlightStringsInText();
        System.out.println("result="+result);
        assertEquals("go away <b>driver</b> of my dream and her <b>driver</b> is cooler", result);
    }
    
    @Test
    public void testStringFinder2() {
        String[] tokens = new String[]{"driver", "her"};
        String src = "go away driver of my dream and her driver is cooler";
        StringHighlighter sf = new StringHighlighter(tokens, src);
        String result = sf.highlightStringsInText();
        System.out.println("result="+result);
        assertEquals("go away <b>driver</b> of my dream and <b>her</b> <b>driver</b> is cooler", result);
    }
    
    @Test
    public void testStringFinder3() {
        String[] tokens = new String[]{"driv"};
        String src = "go away driver of my dream";
        StringHighlighter sf = new StringHighlighter(tokens, src);                
        String result = sf.highlightStringsInText();
        System.out.println("result="+result);
        assertEquals("go away <b>driv</b>er of my dream", result);
    }
    
    @Test
    public void testStringFinder4() {
        String[] tokens = new String[]{"driv"};
        String src = "driver[] drivers = policy.drivers; system.out.println(\"drivers: \" + drivers == null); drivercalc[] drivercalcs = new drivercalc[drivers.length]; for (int i=0; i";
        StringHighlighter sf = new StringHighlighter(tokens, src);                
        String result = sf.highlightStringsInText();
        System.out.println("result="+result);
        assertEquals("<b>driv</b>er[] <b>driv</b>ers = policy.<b>driv</b>ers; system.out.println(\"<b>driv</b>ers: \" + <b>driv</b>ers == null); <b>driv</b>ercalc[] <b>driv</b>ercalcs = new <b>driv</b>ercalc[<b>driv</b>ers.length]; for (int i=0; i", result);
    }
    
    @Test
    public void testStringFinder5() {
        String[] tokens = new String[]{"driv"};
        String src = "if driver has taken driver�s training from a licensed driver training company, then driver";
        StringHighlighter sf = new StringHighlighter(tokens, src);                
        String result = sf.highlightStringsInText();
        System.out.println("result="+result);
        assertEquals("if <b>driv</b>er has taken <b>driv</b>er�s training from a licensed <b>driv</b>er training company, then <b>driv</b>er", result);
    }
    
    @Test
    public void testStringFinder6() {
        String[] tokens = new String[]{"Driv", "driv"};
        String src = "driver-test";
        StringHighlighter sf = new StringHighlighter(tokens, src);                
        String result = sf.highlightStringsInText();
        System.out.println("result="+result);
        assertEquals("<b>driv</b>er-test", result);
    }
    
    @Test
    public void testStringFinder7() {
        String[] tokens = new String[]{"Driv"};
        String src = "driver-test";
        StringHighlighter sf = new StringHighlighter(tokens, src);                
        String result = sf.highlightStringsInText();
        System.out.println("result="+result);
        assertEquals("<b>driv</b>er-test", result);
    }
    
    @Test
    public void testStringFinder8() {
        String[] tokens = new String[]{"driv"};
        String src = "Driver-Data";
        StringHighlighter sf = new StringHighlighter(tokens, src);                
        String result = sf.highlightStringsInText();
        System.out.println("result="+result);
        assertEquals("<b>Driv</b>er-Data", result);
    }
    
    @Test
    public void testStringFinder9() {
        String[] tokens = new String[]{"Driv"};
        String src = "o Senior Driver is a type of ";
        StringHighlighter sf = new StringHighlighter(tokens, src);                
        String result = sf.highlightStringsInText();
        System.out.println("result="+result);
        assertEquals("o Senior <b>Driv</b>er is a type of ", result);
    }
    
    
   

}
