package org.openl.poi.functions;

import org.junit.Assert;
import org.junit.Test;

public class TestAllFunctions {
    @Test
    public void test(){
        try{
            new FunctionsRealizedChecker().testAllFunctions();
            Assert.assertTrue(true);
        }catch (Exception e) {
            Assert.assertTrue(false);
        }
    }
}
