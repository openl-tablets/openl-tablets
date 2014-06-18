package org.openl.rules.datatype.gen;

import org.junit.Test;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DatatypeOpenField;
import org.openl.types.impl.DynamicArrayAggregateInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by dl on 6/18/14.
 */
public class FieldDescriptionTest {

    public static final String DEFAULT_STRING_VALUE = "Default value";
    public static final String DEFAULT_BOOLEAN_VALUE = "true";
    public static final String DEFAULT_INTEGER_VALUE = "25";

    @Test
    public void testDefaultValue_String() {
        FieldDescription field = new FieldDescription(String.class);
        field.setDefaultValueAsString(DEFAULT_STRING_VALUE);
        assertEquals(DEFAULT_STRING_VALUE, field.getDefaultValue());
    }

    @Test
    public void testDefaultValue_Boolean() {
        FieldDescription field = new FieldDescription(Boolean.class);
        field.setDefaultValueAsString(DEFAULT_BOOLEAN_VALUE);
        assertEquals(Boolean.TRUE, field.getDefaultValue());
    }

    @Test
    public void testDefaultValue_Integer() {
        FieldDescription field = new FieldDescription(Integer.class);
        field.setDefaultValueAsString(DEFAULT_INTEGER_VALUE);
        assertEquals(Integer.valueOf(25), field.getDefaultValue());
    }

    @Test
    public void testDefaultValue_DefaultBean() {
        FieldDescription field = new FieldDescription(String.class);
        field.setDefaultValueAsString(FieldDescription.DEFAULT_KEY_WORD);
        assertEquals("Return the default keyword itself", FieldDescription.DEFAULT_KEY_WORD, field.getDefaultValue());

        FieldDescription field1 = new FieldDescription(Boolean.class);
        field1.setDefaultValueAsString(FieldDescription.DEFAULT_KEY_WORD);
        assertEquals("Return the default keyword itself", FieldDescription.DEFAULT_KEY_WORD, field1.getDefaultValue());

        FieldDescription field2 = new FieldDescription(Integer.class);
        field2.setDefaultValueAsString(FieldDescription.DEFAULT_KEY_WORD);
        assertEquals("Return the default keyword itself", FieldDescription.DEFAULT_KEY_WORD, field2.getDefaultValue());
    }

    @Test
    public void testGetJavaClass() {
        try {
            FieldDescription.getNotNullType(null);
            fail();
        } catch(NullPointerException ex) {
            assertTrue(true);
        }
        // Create the IOpenClass for the policy
        //
        DatatypeOpenClass policyClass = new DatatypeOpenClass(null, "Policy", "org.openl.generated.beans");

        // Create the IOpenClass for the Driver
        //
        DatatypeOpenClass driverClass = new DatatypeOpenClass(null, "Driver", "org.openl.generated.beans");

        // Create the IOpenClass for the drivers[]
        //
        IOpenClass driversClass = DynamicArrayAggregateInfo.aggregateInfo.getIndexedAggregateType(driverClass, 1);

        // Create the field that belongs to the policy and contains drivers
        //
        DatatypeOpenField driversField = new DatatypeOpenField(policyClass, "drivers", driversClass);

        FieldDescription field = new FieldDescription(driversField);
        assertNull("Null type for the fields that represents DatatypeOpenField for the array",
                field.getType());

        assertEquals(Object.class, FieldDescription.getNotNullType(field));

    }

    @Test
    public void testDatatypeFieldTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>("test/rules/datatype/DatatypeFieldTest.xlsx");
        engineFactory.setExecutionMode(true);

        Class<?> interfaceClass = engineFactory.getInterfaceClass();
    }


}
