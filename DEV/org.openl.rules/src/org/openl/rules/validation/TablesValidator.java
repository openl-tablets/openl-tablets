package org.openl.rules.validation;

import java.util.HashSet;
import java.util.Set;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.types.IOpenClass;
import org.openl.validation.IOpenLValidator;
import org.openl.validation.ValidationResult;

public abstract class TablesValidator implements IOpenLValidator {

    /** The tables of the module and of every module it depends on, the tables the engine builds left out. */
    private static Set<TableSyntaxNode> findAllTableSyntaxNodes(IOpenClass openClass) {
        var tableSyntaxNodes = new HashSet<TableSyntaxNode>();
        for (var module : ValidationUtils.modulesOf(openClass)) {
            var xlsMetaInfo = module.getXlsMetaInfo();
            if (xlsMetaInfo != null) {
                for (TableSyntaxNode tableSyntaxNode : xlsMetaInfo.getXlsModuleNode().getXlsTableSyntaxNodes()) {
                    if (!DispatcherTablesBuilder.isDispatcherTable(tableSyntaxNode)) {
                        tableSyntaxNodes.add(tableSyntaxNode);
                    }
                }
            }
        }
        return tableSyntaxNodes;
    }

    @Override
    public ValidationResult validate(IOpenClass openClass) {

        if (openClass instanceof XlsModuleOpenClass) {

            // Get all table syntax nodes of xls module.
            //
            var tableSyntaxNodes = findAllTableSyntaxNodes(openClass);

            return validateTables(tableSyntaxNodes.toArray(TableSyntaxNode.EMPTY_ARRAY), openClass);
        }

        // Skip validation if passed open class is not instance of
        // XlsModuleOpenClass.
        //
        return ValidationUtils.validationSuccess();
    }

    public abstract ValidationResult validateTables(TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass);
}
