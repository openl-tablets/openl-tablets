package org.openl.rules;

import java.io.File;

import org.junit.Ignore;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.runtime.RuleEngineFactory;
import org.openl.runtime.EngineFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.types.impl.DynamicObject;

@Ignore("Auxiliary class")
public class TestHelper<T> {

    private EngineFactory<T> engineFactory;
    private T instance;
    private TableSyntaxNode tableSyntaxNode;

    public TestHelper(File file, Class<T> tClass) {
        engineFactory = new RuleEngineFactory<T>(file, tClass);

        instance = engineFactory.makeInstance();

        IEngineWrapper ew = (IEngineWrapper) instance;
        DynamicObject dObj = (DynamicObject) ew.getInstance();
        XlsMetaInfo xlsMI = (XlsMetaInfo) dObj.getType().getMetaInfo();
        tableSyntaxNode =  xlsMI.getXlsModuleNode().getXlsTableSyntaxNodes()[0];
    }

    public EngineFactory<T> getEngineFactory() {
        return engineFactory;
    }

    public T getInstance() {
        return instance;
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return tableSyntaxNode;
    }
}
