package org.openl.rules.datatype;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
    public void testPackageGen() throws ClassNotFoundException {
        assertNotNull(getClass("org.table.Driver"));
        assertNotNull(getClass("org.modue.package.Policy"));
        assertNotNull(getClass("org.modue.package.Vehicle"));
        assertNotNull(getClass("Org.Table.TEST.ContainsCapitalLetters"));
    }

    private boolean hasErrorInPackageName(TableSyntaxNode tsn) {
        if (tsn.hasErrors()) {
            for (SyntaxNodeException exception : tsn.getErrors()) {
                if (exception.getMessage().matches("Incorrect value .+ for property \"Datatype Package\"")) {
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
        assertFalse(hasErrorInPackageName(findTable("Datatype ContainsCapitalLetters")));
    }

    private Class<?> getClass(String name) throws ClassNotFoundException {
        Class<?> clazz = getCompiledOpenClass().getClassLoader().loadClass(name);
        assertNotNull(clazz);
        return clazz;
    }
}
