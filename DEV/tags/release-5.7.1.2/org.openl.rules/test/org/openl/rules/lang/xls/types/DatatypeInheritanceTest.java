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

/**
 * 
 * @author PUdalau
 */
public class DatatypeInheritanceTest extends BaseOpenlBuilderHelper {
    private static String __src = "test/rules/DatatypeInheritanceTest.xls";
    private static final String PARENT_TYPE_NAME = ISyntaxConstants.THIS_NAMESPACE + ".ParentType";
    private static final String CHILD_TYPE_NAME = ISyntaxConstants.THIS_NAMESPACE + ".ChildType";
    private static final String SECOND_CHILD_TYPE_NAME = ISyntaxConstants.THIS_NAMESPACE + ".SecondLevelChildType";

    public DatatypeInheritanceTest() {
        super(__src);
    }

    @Test
    public void testFieldsAccess() {
        XlsModuleOpenClass moduleOpenClass = (XlsModuleOpenClass) getJavaWrapper().getOpenClassWithErrors();
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

}
