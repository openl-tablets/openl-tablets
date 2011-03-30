package org.openl.rules.datatype;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;

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
    
    private boolean hasErrorInPackageName(TableSyntaxNode tsn) {
        if (tsn.hasErrors()) {
            for (SyntaxNodeException exception : tsn.getErrors()) {
                if (exception.getMessage().equals("Incorrect value for property \"datatypePackage\"")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    public void testPackageNames() {
        assertTrue(hasErrorInPackageName(findTable("Datatype DotEnded")));
        assertTrue(hasErrorInPackageName(findTable("Datatype WithSpecialSymbols")));
        assertTrue(hasErrorInPackageName(findTable("Datatype WithSpecialSymbols2")));
        assertTrue(hasErrorInPackageName(findTable("Datatype StartsWithDigit")));
        assertFalse(hasErrorInPackageName(findTable("Datatype Driver")));
    }
}
