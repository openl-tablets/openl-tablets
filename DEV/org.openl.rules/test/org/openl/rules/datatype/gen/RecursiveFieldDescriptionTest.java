package org.openl.rules.datatype.gen;

import org.junit.Test;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DatatypeOpenField;
import org.openl.types.impl.DynamicArrayAggregateInfo;

import static org.junit.Assert.assertEquals;

/**
 * Created by dl on 6/19/14.
 */
public class RecursiveFieldDescriptionTest {
    @Test
    public void testGetTypeWriter_RecursionField() {
        // Create the IOpenClass for the policy
        //
        DatatypeOpenClass policyClass = new DatatypeOpenClass(Policy.class.getSimpleName(), Policy.class.getPackage().getName());
        policyClass.setInstanceClass(Policy.class);

        // Create the IOpenClass for the Driver
        // NOTE! Without instance class, to simulate the situation with
        // the recursive field in the datatype
        //
        DatatypeOpenClass driverClass = new DatatypeOpenClass("Driver", "org.openl.beans.generated");

        // Create the IOpenClass for the drivers[]
        //
        IOpenClass driversClass = DynamicArrayAggregateInfo.aggregateInfo.getIndexedAggregateType(driverClass, 1);

        // Create the field that belongs to the policy and contains drivers
        //
        DatatypeOpenField driversField = new DatatypeOpenField(policyClass, "drivers", driversClass);

        FieldDescription field = new RecursiveFieldDescription(driversField);

        assertEquals("Object class is used as a type, as instance class is unknown", Object.class, field.getType());
        assertEquals("Canonical name based on the package", "org.openl.beans.generated.Driver[]", field.getCanonicalTypeName());
    }

    private class Policy {

    }
}
