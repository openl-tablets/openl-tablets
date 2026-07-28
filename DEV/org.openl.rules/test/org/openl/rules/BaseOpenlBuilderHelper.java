package org.openl.rules;

import lombok.Getter;

import org.openl.CompiledOpenClass;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.types.IOpenMethod;

/**
 * Helper class for building IOpenClass and getting XlsModuleSyntaxNode from it. To get everything you need for your
 * tests just extend this class.
 *
 * @author DLiauchuk
 * @deprecated Use {@link TestUtils} instead
 */
@Deprecated
public abstract class BaseOpenlBuilderHelper {

    protected Object instance;
    @Getter
    private final CompiledOpenClass compiledOpenClass;

    public BaseOpenlBuilderHelper(String src) {
        var engineFactory = new RulesEngineFactory<Object>(src);
        compiledOpenClass = engineFactory.getCompiledOpenClass();
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
        var moduleOpenClass = getCompiledOpenClass().getOpenClass();
        for (IOpenMethod method : moduleOpenClass.getMethods()) {
            if (method.getInfo() != null && method.getInfo().getSyntaxNode() instanceof TableSyntaxNode) {
                var tsn = (TableSyntaxNode) method.getInfo().getSyntaxNode();
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
