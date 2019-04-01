package org.openl.rules.ui.tree;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.types.IOpenMethod;

public class TestVersionedTreeNode {
    private Map<String, TableSyntaxNode> tables = new HashMap<>();

    @Before
    public void getTables() {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<>("./test/rules/Versions_Test.xls");
        CompiledOpenClass compiledOpenClass = engineFactory.getCompiledOpenClass();
        XlsMetaInfo xmi = (XlsMetaInfo) compiledOpenClass.getOpenClassWithErrors().getMetaInfo();
        XlsModuleSyntaxNode xsn = xmi.getXlsModuleNode();
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
