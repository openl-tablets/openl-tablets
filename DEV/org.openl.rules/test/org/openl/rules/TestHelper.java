package org.openl.rules;

import java.io.File;
import java.net.URL;

import org.junit.Ignore;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.runtime.EngineFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.types.impl.DynamicObject;

@Ignore("Auxiliary class")
public class TestHelper<T> {

    private RulesEngineFactory<T> engineFactory;
    private T instance;
    private TableSyntaxNode tableSyntaxNode;

    public TestHelper(File file, Class<T> tClass) {
        engineFactory = new RulesEngineFactory<T>(URLSourceCodeModule.toUrl(file), tClass);

        instance = engineFactory.newEngineInstance();

        IEngineWrapper ew = (IEngineWrapper) instance;
        DynamicObject dObj = (DynamicObject) ew.getInstance();
        XlsMetaInfo xlsMI = (XlsMetaInfo) dObj.getType().getMetaInfo();
        tableSyntaxNode =  xlsMI.getXlsModuleNode().getXlsTableSyntaxNodes()[0];
    }

    public TestHelper(File file, Class<T> tClass, boolean executionMode) {
        engineFactory = new RulesEngineFactory<T>(URLSourceCodeModule.toUrl(file), tClass);
        engineFactory.setExecutionMode(executionMode);

        instance = engineFactory.newEngineInstance();

        IEngineWrapper ew = (IEngineWrapper) instance;
        DynamicObject dObj = (DynamicObject) ew.getInstance();
        XlsMetaInfo xlsMI = (XlsMetaInfo) dObj.getType().getMetaInfo();
        if (!executionMode)
        	tableSyntaxNode =  xlsMI.getXlsModuleNode().getXlsTableSyntaxNodes()[0];
    }
    
    

    public TestHelper(URL url, Class<T> tClass) {
        engineFactory = new RulesEngineFactory<T>(new URLSourceCodeModule(url), tClass);

        instance = engineFactory.newEngineInstance();

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
