package org.openl.rules.datatype;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;

public class DatatypePackageGenerationTest extends BaseOpenlBuilderHelper {

    private static String src = "test/rules/DatatypePackageGenerationTest.xls";
    
    public DatatypePackageGenerationTest() {
        super(src);
    }
    
    @Test
    public void testPackageGen() {
        try {
            assertNotNull("Check that there is class Driver with appropriate package", Class.forName("org.table.Driver"));
            assertNotNull("Check that there is class Policy with appropriate package", Class.forName("org.modue.package.Policy"));
            assertNotNull("Check that there is class Vehicle with appropriate package", Class.forName("org.modue.package.Vehicle"));
        } catch (ClassNotFoundException e) {
           fail();
        }
    }

    
}
