package org.openl.rules.lang.xls.types;

import java.util.Map;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessages;
import org.openl.message.Severity;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;
import org.openl.util.StringTool;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

/**
 * 
 * @author PUdalau
 */
public class DatatypeInheritanceTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "test/rules/DatatypeInheritanceTest.xls";
    private static final String PARENT_TYPE_NAME = StringTool.buildTypeName(ISyntaxConstants.THIS_NAMESPACE, "ParentType");
    private static final String CHILD_TYPE_NAME = StringTool.buildTypeName(ISyntaxConstants.THIS_NAMESPACE, "ChildType");
    private static final String SECOND_CHILD_TYPE_NAME = StringTool.buildTypeName(ISyntaxConstants.THIS_NAMESPACE, "SecondLevelChildType");

    public DatatypeInheritanceTest() {
        super(SRC);
    }

    @Test
    public void testFieldsAccess() {
        XlsModuleOpenClass moduleOpenClass = (XlsModuleOpenClass) getCompiledOpenClass().getOpenClassWithErrors();
        Map<String, IOpenClass> types = moduleOpenClass.getTypes();
        assertNotNull(types.get(PARENT_TYPE_NAME).getField("field2"));

        assertNotNull(types.get(CHILD_TYPE_NAME).getField("field1"));
        assertEquals(types.get(PARENT_TYPE_NAME), types.get(CHILD_TYPE_NAME).getField("field3").getDeclaringClass());
        assertEquals(types.get(CHILD_TYPE_NAME), types.get(CHILD_TYPE_NAME).getField("field5").getDeclaringClass());

        assertNotNull(types.get(SECOND_CHILD_TYPE_NAME).getField("field1"));
        assertEquals(types.get(PARENT_TYPE_NAME), types.get(SECOND_CHILD_TYPE_NAME).getField("field3")
                .getDeclaringClass());
        assertEquals(types.get(CHILD_TYPE_NAME), types.get(SECOND_CHILD_TYPE_NAME).getField("field5")
                .getDeclaringClass());
        assertEquals(types.get(SECOND_CHILD_TYPE_NAME), types.get(SECOND_CHILD_TYPE_NAME).getField("field7")
                .getDeclaringClass());
    }

    @Test
    public void testWarning() {
        boolean wasFound = false;
        for (OpenLMessage message : OpenLMessages.getCurrentInstance().getMessages()) {
            if (message.getSeverity() == Severity.WARN) {
                if (message.getSummary().equals("Field [field1] has been already defined in class \"ParentType\"")) {
                    wasFound = true;
                }
            }
        }
        assertTrue(wasFound);
    }

    @Test
    public void testError() {
        boolean wasFound = false;
        for (OpenLMessage message : OpenLMessages.getCurrentInstance().getMessages()) {
            if (message.getSeverity() == Severity.ERROR) {
                if (message.getSummary().equals(
                        "Field [field1] has been already defined in class \"ParentType\" with another type")) {
                    wasFound = true;
                }
            }
        }
        assertTrue(wasFound);
    }

    @Test
    public void testToStringMethod() {
        XlsModuleOpenClass moduleOpenClass = (XlsModuleOpenClass) getCompiledOpenClass().getOpenClassWithErrors();
        Map<String, IOpenClass> types = moduleOpenClass.getTypes();
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        IOpenClass childType = types.get(CHILD_TYPE_NAME);
        String childTypeToStringResult = (String)childType.getMethod("toString", new IOpenClass[] {}).invoke(
                childType.newInstance(env), new Object[] {}, env);
        assertTrue(childTypeToStringResult.contains("field5"));
        assertTrue(childTypeToStringResult.startsWith("ChildType"));
        IOpenClass secondLevelChildType = types.get(SECOND_CHILD_TYPE_NAME);
        String secondLevelChildTypeToStringResult = (String)secondLevelChildType.getMethod("toString", new IOpenClass[] {}).invoke(
                secondLevelChildType.newInstance(env), new Object[] {}, env);
        assertTrue(secondLevelChildTypeToStringResult.contains("field7"));
        assertTrue(secondLevelChildTypeToStringResult.startsWith("SecondLevelChildType"));
    }
}
