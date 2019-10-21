package org.openl.rules.lang.xls.types;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

/**
 *
 * @author PUdalau
 */
public class DatatypeInheritanceTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "test/rules/DatatypeInheritanceTest.xls";

    public DatatypeInheritanceTest() {
        super(SRC);
    }

    @Test
    public void testFieldsAccess() {
        XlsModuleOpenClass moduleOpenClass = (XlsModuleOpenClass) getCompiledOpenClass().getOpenClassWithErrors();
        IOpenClass parentType = moduleOpenClass.findType("ParentType");
        IOpenClass childType = moduleOpenClass.findType("ChildType");
        IOpenClass secondLevelChildType = moduleOpenClass.findType("SecondLevelChildType");
        assertNotNull(parentType.getField("field2"));

        assertNotNull(childType.getField("field1"));
        assertEquals(parentType, childType.getField("field3").getDeclaringClass());
        assertEquals(childType, childType.getField("field5").getDeclaringClass());

        assertNotNull(secondLevelChildType.getField("field1"));
        assertEquals(parentType, secondLevelChildType.getField("field3").getDeclaringClass());
        assertEquals(childType, secondLevelChildType.getField("field5").getDeclaringClass());
        assertEquals(secondLevelChildType, secondLevelChildType.getField("field7").getDeclaringClass());
    }

    @Test
    public void testWarning() {
        boolean wasFound = false;
        for (OpenLMessage message : getCompiledOpenClass().getMessages()) {
            if (message.getSeverity() == Severity.WARN) {
                if (message.getSummary().equals("Field [field1] has been already defined in class 'ParentType'")) {
                    wasFound = true;
                }
            }
        }
        assertTrue(wasFound);
    }

    @Test
    public void testError() {
        boolean wasFound = false;
        for (OpenLMessage message : getCompiledOpenClass().getMessages()) {
            if (message.getSeverity() == Severity.ERROR) {
                if (message.getSummary()
                    .equals("Field [field1] has been already defined in class 'ParentType' with another type")) {
                    wasFound = true;
                }
            }
        }
        assertTrue(wasFound);
    }

    @Test
    public void testToStringMethod() {
        XlsModuleOpenClass moduleOpenClass = (XlsModuleOpenClass) getCompiledOpenClass().getOpenClassWithErrors();
        IOpenClass childType = moduleOpenClass.findType("ChildType");
        IOpenClass secondLevelChildType = moduleOpenClass.findType("SecondLevelChildType");

        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        String childTypeToStringResult = (String) childType.getMethod("toString", new IOpenClass[] {})
            .invoke(childType.newInstance(env), new Object[] {}, env);
        assertTrue(childTypeToStringResult.contains("field5"));
        assertTrue(childTypeToStringResult.startsWith("ChildType"));
        String secondLevelChildTypeToStringResult = (String) secondLevelChildType
            .getMethod("toString", new IOpenClass[] {})
            .invoke(secondLevelChildType.newInstance(env), new Object[] {}, env);
        assertTrue(secondLevelChildTypeToStringResult.contains("field7"));
        assertTrue(secondLevelChildTypeToStringResult.startsWith("SecondLevelChildType"));
    }
}
