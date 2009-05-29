package org.openl.rules.test.liveexcel.formula;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.openl.rules.liveexcel.formula.DeclareSearcher;
import org.openl.rules.liveexcel.formula.DeclaredFunction;


public class DeclareSearchTest {

    @Test
    public void testDeclare() {
        DeclareSearcher searcher = new DeclareSearcher("./test/resources/Functions_2009.05.18.xls");
        searcher.findFunctions();
        List<DeclaredFunction> declFunctions= searcher.getFunctionsToParse();
        List<String> finalIndexes = new ArrayList<String>();
        for(DeclaredFunction decFun : declFunctions) {
            finalIndexes.add(decFun.getCellAdress());
        }
        
        List<String> expextedIndexes = new ArrayList<String>();
        expextedIndexes.add("B3");
        expextedIndexes.add("B10");
        expextedIndexes.add("B16");        
        
        assertTrue("All Declared functions were found",expextedIndexes.containsAll(finalIndexes));        
    }

}



    


