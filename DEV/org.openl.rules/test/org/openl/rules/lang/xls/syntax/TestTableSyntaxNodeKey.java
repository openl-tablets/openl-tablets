package org.openl.rules.lang.xls.syntax;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openl.OpenL;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.types.IOpenMethod;

public class TestTableSyntaxNodeKey {
    private final static String SRC = "test/rules/OverloadedTables_Test.xls";
    private XlsModuleSyntaxNode xsn = null;
    private List<TableSyntaxNode> driverAgeTypeTables = new ArrayList<TableSyntaxNode>();
    private List<TableSyntaxNode> driverEligibilityTables = new ArrayList<TableSyntaxNode>();

    @Before
    public void getTables() {
        OpenClassJavaWrapper wrapper = getJavaWrapper();
        XlsMetaInfo xmi = (XlsMetaInfo) wrapper.getOpenClassWithErrors().getMetaInfo();
        xsn = xmi.getXlsModuleNode();
        TableSyntaxNode[] tsns = xsn.getXlsTableSyntaxNodes();
        for (TableSyntaxNode tsn : tsns) {
            if (tsn.getMember() instanceof IOpenMethod && tsn.getType().equals("xls.dt")) {
                if (tsn.getMember().getName().equals("driverAgeType")) {
                    driverAgeTypeTables.add(tsn);
                } else if (tsn.getMember().getName().equals("driverEligibility")) {
                    driverEligibilityTables.add(tsn);
                }
            }
        }
    }

    private OpenClassJavaWrapper getJavaWrapper() {
        UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), ".");
        OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper(OpenL.OPENL_JAVA_RULE_NAME, ucxt, SRC);
        return wrapper;
    }

    @Test
    public void testEquals() {
        assertTrue(new TableSyntaxNodeKey(driverAgeTypeTables.get(0)).equals(new TableSyntaxNodeKey(driverAgeTypeTables
                .get(1))));
        assertTrue(!new TableSyntaxNodeKey(driverEligibilityTables.get(0)).equals(new TableSyntaxNodeKey(
                driverEligibilityTables.get(1))));

    }

    @Test
    public void testHashCode() {
        // same hash codes for equal objects
        assertEquals(new TableSyntaxNodeKey(driverAgeTypeTables.get(0)).hashCode(), new TableSyntaxNodeKey(
                driverAgeTypeTables.get(1)).hashCode());
    }
}
