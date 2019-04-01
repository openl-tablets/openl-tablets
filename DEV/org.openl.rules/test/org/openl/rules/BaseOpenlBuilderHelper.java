package org.openl.rules;

import org.openl.CompiledOpenClass;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.runtime.EngineFactory;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

/**
 * Helper class for building IOpenClass and getting XlsModuleSyntaxNode from it. To get everything you need for your
 * tests just extend this class.
 *
 *
 * @author DLiauchuk
 *
 * @deprecated Use {@link TestUtils} instead
 *
 */
@Deprecated
public abstract class BaseOpenlBuilderHelper {

    protected Object instance;
    private CompiledOpenClass compiledOpenClass;

    public BaseOpenlBuilderHelper(String src) {
        EngineFactory<Object> engineFactory = new RulesEngineFactory<>(src);
        compiledOpenClass = engineFactory.getCompiledOpenClass();
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
        return ((XlsMetaInfo) compiledOpenClass.getOpenClassWithErrors().getMetaInfo()).getXlsModuleNode()
            .getXlsTableSyntaxNodes();
    }
}
