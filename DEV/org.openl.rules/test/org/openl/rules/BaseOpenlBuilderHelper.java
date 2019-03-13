package org.openl.rules;

import org.openl.CompiledOpenClass;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.runtime.EngineFactory;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.SimpleVM.SimpleRuntimeEnv;

/**
 * Helper class for building IOpenClass and getting XlsModuleSyntaxNode from it. To get everything you need for your
 * tests just extend this class.
 * 
 * 
 * @author DLiauchuk
 *
 */
public abstract class BaseOpenlBuilderHelper {

    private XlsModuleSyntaxNode xsn;
    private CompiledOpenClass compiledOpenClass;
    protected Object instance;

    public BaseOpenlBuilderHelper(String src) {
        EngineFactory<Object> engineFactory = new RulesEngineFactory<>(src);
        engineFactory.setExecutionMode(false);
        compiledOpenClass = engineFactory.getCompiledOpenClass();
        XlsMetaInfo xmi = (XlsMetaInfo) compiledOpenClass.getOpenClassWithErrors().getMetaInfo();
        xsn = xmi.getXlsModuleNode();
    }

    public Object newInstance() {
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        return getCompiledOpenClass().getOpenClass().newInstance(env);
    }

    public CompiledOpenClass getCompiledOpenClass() {
        return compiledOpenClass;
    }

    protected TableSyntaxNode findTable(String tableName) {
        TableSyntaxNode result = null;
        for (TableSyntaxNode tsn : getTableSyntaxNodes()) {
            if (tableName.equals(tsn.getDisplayName())) {
                result = tsn;
            }
        }
        return result;
    }

    protected TableSyntaxNode findDispatcherForMethod(String methodName) {
        IOpenClass moduleOpenClass = getCompiledOpenClass().getOpenClass();
        for (IOpenMethod method : moduleOpenClass.getMethods()) {
            if (method.getInfo() != null && method.getInfo().getSyntaxNode() instanceof TableSyntaxNode) {
                TableSyntaxNode tsn = (TableSyntaxNode) method.getInfo().getSyntaxNode();
                if (DispatcherTablesBuilder.isDispatcherTable(tsn) && method.getName().endsWith(methodName)) {
                    return tsn;
                }
            }
        }
        return null;
    }

    protected TableSyntaxNode[] getTableSyntaxNodes() {
        return xsn.getXlsTableSyntaxNodes();
    }

}
