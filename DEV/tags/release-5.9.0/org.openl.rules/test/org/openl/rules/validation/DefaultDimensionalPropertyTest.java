package org.openl.rules.validation;

import org.openl.CompiledOpenClass;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.types.IOpenClass;

import org.junit.Test;

public class DefaultDimensionalPropertyTest extends BaseOpenlBuilderHelper{

    
    private static String __src = "test/rules/validation/TestPropertyValidation.xls";

    public DefaultDimensionalPropertyTest() {
        super(__src);
    }
    
    @Test
    public void testError()
    {
        CompiledOpenClass coc = getJavaWrapper().getCompiledClass();
        IOpenClass ioc =  getJavaWrapper().getOpenClass();
    }
    
    

}
