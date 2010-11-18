package org.openl.rules.dependency.modules;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

public class ModuleDependencyTest extends BaseOpenlBuilderHelper {
    
    private static String src = "test/rules/moduleDependency/main/RootProject.xls";
    
    public ModuleDependencyTest() {
        super(src);
    }
    
    @Before
    public void before() {
        testNoErrors();
    }
    
    @Test
    public void testDependencyNumber() {
        Set<CompiledOpenClass> dep = ((XlsModuleOpenClass)getJavaWrapper().getOpenClass()).getDependencies();
        assertEquals("Only one dependency project", 1, dep.size());
    }
    
    @Test
    public void testDatatypes() {
        Map<String, IOpenClass> types = getJavaWrapper().getOpenClass().getTypes();
        assertEquals("2 datatype in module", 2, types.size());
        
        Set<CompiledOpenClass> dep = ((XlsModuleOpenClass)getJavaWrapper().getOpenClass()).getDependencies();
        assertEquals("Only one dependency project", 1, dep.size());
        for (CompiledOpenClass dependency : dep) {
            assertTrue("One datatype from dependency module", dependency.getTypes().size() == 1);
        }
    }
    
    @Test
    public void testDependencyFields() {
        Map<String, IOpenField> fields = getJavaWrapper().getOpenClass().getFields();   
        assertEquals(19, fields.size());
        assertTrue("Contains field from dependent module", fields.containsKey("driverProfiles1"));  
        
        Set<CompiledOpenClass> dep = ((XlsModuleOpenClass)getJavaWrapper().getOpenClass()).getDependencies();
        assertEquals("Only one dependency project", 1, dep.size());
        for (CompiledOpenClass dependency : dep) {
            assertEquals("2 fields in dependent module", 2, dependency.getOpenClass().getFields().size());
            assertTrue("Driver profile 1 belongs to this module", dependency.getOpenClass().getFields().containsKey("driverProfiles1"));
        }
    }
    
    @Test
    public void testInvokeDependencymethod() {        
        Object result = invokeMethod("start");
        assertEquals("Good Afternoon", result.toString());        
    }
    
    @Test
    public void testInvokeDependencyField() {        
        Object result = invokeMethod("startField");
        assertEquals("Sara", result.toString());
    }

    @SuppressWarnings("deprecation")
    private void testNoErrors() {
        Assert.assertTrue("No binding errors", getJavaWrapper().getCompiledClass().getBindingErrors().length == 0);
        Assert.assertTrue("No parsing errors", getJavaWrapper().getCompiledClass().getParsingErrors().length == 0);
        Assert.assertTrue("No warnings", getJavaWrapper().getCompiledClass().getMessages().size() == 0);
        
    }
}
