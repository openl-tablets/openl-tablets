package org.openl.rules.validation;

import java.util.HashSet;
import java.util.Set;

import org.openl.dependency.CompiledDependency;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.types.IOpenClass;
import org.openl.validation.IOpenLValidator;
import org.openl.validation.ValidationResult;

public abstract class TablesValidator implements IOpenLValidator {

    private void findAllTableSyntaxNodes(Set<TableSyntaxNode> tableSyntaxNodes, IOpenClass openClass) {
        if (openClass instanceof XlsModuleOpenClass) {
            for (CompiledDependency compiledDependency : ((XlsModuleOpenClass) openClass).getDependencies()) {
                IOpenClass dependencyOpenClass = compiledDependency.getCompiledOpenClass().getOpenClassWithErrors();
                findAllTableSyntaxNodes(tableSyntaxNodes, dependencyOpenClass);
            }
            XlsMetaInfo xlsMetaInfo = ((XlsModuleOpenClass) openClass).getXlsMetaInfo();
            if (xlsMetaInfo != null) {
                TableSyntaxNode[] xlsTableSyntaxNodes = xlsMetaInfo.getXlsModuleNode().getXlsTableSyntaxNodes();
                for (TableSyntaxNode tableSyntaxNode : xlsTableSyntaxNodes) {
                    if (!DispatcherTablesBuilder.isDispatcherTable(tableSyntaxNode)) {
                        tableSyntaxNodes.add(tableSyntaxNode);
                    }
                }
            }
        }
    }

    @Override
    public ValidationResult validate(IOpenClass openClass) {

        if (openClass instanceof XlsModuleOpenClass) {

            // Get all table syntax nodes of xls module.
            //
            Set<TableSyntaxNode> tableSyntaxNodes = new HashSet<>();

            findAllTableSyntaxNodes(tableSyntaxNodes, openClass);

            return validateTables(tableSyntaxNodes.toArray(new TableSyntaxNode[]{}), openClass);
        }

        // Skip validation if passed open class is not instance of
        // XlsModuleOpenClass.
        //
        return ValidationUtils.validationSuccess();
    }

    public abstract ValidationResult validateTables(TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass);
}
