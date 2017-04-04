package org.openl.rules.ui.tree;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.types.IOpenMethod;

public class TestVersionedTreeNode {
    private String __src = "test/rules/Versions_Test.xls";
    private XlsModuleSyntaxNode xsn = null;
    private Map<String, TableSyntaxNode> tables = new HashMap<String, TableSyntaxNode>();

    @Before
    public void getTables() {
        UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), ".");
        CompiledOpenClass wrapper = OpenClassJavaWrapper.createWrapper(OpenL.OPENL_JAVA_RULE_NAME, ucxt, __src, null);
        XlsMetaInfo xmi = (XlsMetaInfo) wrapper.getOpenClassWithErrors().getMetaInfo();
        xsn = xmi.getXlsModuleNode();
        TableSyntaxNode[] tsns = xsn.getXlsTableSyntaxNodes();
        for (TableSyntaxNode tsn : tsns) {
            if (tsn.getMember() instanceof IOpenMethod && tsn.getType().equals("xls.dt")) {
                if (tsn.getMember().getName().equals("hello1")) {
                    tables.put(tsn.getTableProperties().getVersion(), tsn);
                }
            }
        }
    }

    @Test
    public void testVersionsComparing() {
        assertTrue(VersionedTreeNode.findLaterTable(tables.get("1.1.0"), tables.get("1.2.0")) < 0);
        assertTrue(VersionedTreeNode.findLaterTable(tables.get("1.1.0"), tables.get("0.1.2")) < 0);
        assertTrue(VersionedTreeNode.findLaterTable(tables.get("0.1.2"), tables.get("1.2.0")) > 0);
    }
}
