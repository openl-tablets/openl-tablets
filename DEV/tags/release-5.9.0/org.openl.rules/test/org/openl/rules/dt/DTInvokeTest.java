package org.openl.rules.dt;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

public class DTInvokeTest extends BaseOpenlBuilderHelper {
    
    public static final String src = "test/rules/dt/DTInvokeTest.xls";
    
    public DTInvokeTest() {
        super(src);
    }
    
    @Test
    public void testInvoking() {
        String methodName = "getILFactor";
        
        IOpenMethod method = getMethod(methodName, new IOpenClass[]{JavaOpenClass.STRING, JavaOpenClass.STRING});
        
        DoubleValue res = (DoubleValue)invokeMethod(method, new Object[]{"Comp", "PA"});
        assertEquals(1, res.intValue());
        
        DoubleValue res1 = (DoubleValue)invokeMethod(method, new Object[]{"Coll", "PA"});
        assertEquals(2, res1.intValue());
        
        DoubleValue res2 = (DoubleValue)invokeMethod(method, new Object[]{"Comp", "MH"});
        assertEquals(3, res2.intValue());
        
        DoubleValue res3 = (DoubleValue)invokeMethod(method, new Object[]{"Comp", "TR"});
        assertEquals(4, res3.intValue());
        
        DoubleValue res4 = (DoubleValue)invokeMethod(method, new Object[]{"Any", "GH"});
        assertEquals(5, res4.intValue());
    }
}
