package org.openl.rules.validation.properties.dimentional;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.MethodSignature;
import org.openl.types.java.JavaOpenClass;

import static org.junit.Assert.*;

public class DispatcherTableReturnColumnTest {
    
    @Test
    public void testGetparameterDeclaration() {
        DispatcherTableReturnColumn retColumn = new DispatcherTableReturnColumn();
        IOpenClass originalReturnType = JavaOpenClass.FLOAT;
        retColumn.setOriginalReturnType(originalReturnType);
        
        assertEquals("float result", retColumn.getParameterDeclaration());
        
        retColumn.setOriginalReturnType(NullOpenClass.the);
        assertEquals("null-Class result", retColumn.getParameterDeclaration());
    }
    
    @Test 
    public void testGetCodeExpression() {
        DispatcherTableReturnColumn retColumn = new DispatcherTableReturnColumn();
        assertEquals("result", retColumn.getCodeExpression());
    }
    
    @Test
    public void testGetTitle() {
        DispatcherTableReturnColumn retColumn = new DispatcherTableReturnColumn();
        assertEquals("RESULT", retColumn.getTitle());
    }
    
    @Test
    public void testGetOriginalParamsThroughComma() {
        DispatcherTableReturnColumn retColumn = new DispatcherTableReturnColumn();
        IMethodSignature signature = new MethodSignature(new IOpenClass[]{JavaOpenClass.STRING, JavaOpenClass.FLOAT});
        retColumn.setOriginalSignature(signature);
        
        assertEquals("p0, p1", retColumn.originalParamsThroughComma());
        
        signature = new MethodSignature(new IOpenClass[0]);
        retColumn.setOriginalSignature(signature);
        assertEquals(StringUtils.EMPTY, retColumn.originalParamsThroughComma());
    }
    
    @Test
    public void testParamsThroughComma() {
        DispatcherTableReturnColumn retColumn = new DispatcherTableReturnColumn();
        IMethodSignature signature = new MethodSignature(new IOpenClass[]{JavaOpenClass.STRING, JavaOpenClass.FLOAT});
        
        Map<String, IOpenClass> newIncomeParams = new HashMap<String, IOpenClass>();
        newIncomeParams.put("field1", JavaOpenClass.STRING);
        newIncomeParams.put("field2", JavaOpenClass.INT);
        newIncomeParams.put("field3", JavaOpenClass.DOUBLE);
        
        retColumn.setOriginalSignature(signature);
        retColumn.setNewIncomeParams(newIncomeParams);
        assertEquals("String p0, float p1, double field3, int field2, String field1", retColumn.paramsThroughComma());
        
        // check with empty signature
        //
        retColumn.setOriginalSignature(new MethodSignature(new IOpenClass[0]));
        
        assertEquals("double field3, int field2, String field1", retColumn.paramsThroughComma());
        
        // check with empty new income parameters
        //
        retColumn.setNewIncomeParams(new HashMap<String, IOpenClass>());
        signature = new MethodSignature(new IOpenClass[]{JavaOpenClass.DOUBLE, JavaOpenClass.SHORT});
        retColumn.setOriginalSignature(signature);
        
        assertEquals("double p0, short p1", retColumn.paramsThroughComma());
        
        // check with both empty signature and income parameters
        //        
        retColumn.setOriginalSignature(new MethodSignature(new IOpenClass[0]));
        retColumn.setNewIncomeParams(new HashMap<String, IOpenClass>());
        assertEquals(StringUtils.EMPTY, retColumn.paramsThroughComma());
        
    }
}
