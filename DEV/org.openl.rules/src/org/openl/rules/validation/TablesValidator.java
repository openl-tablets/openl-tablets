package org.openl.rules.validation;

import java.util.ArrayList;
import java.util.List;

import org.openl.OpenL;
import org.openl.dependency.CompiledDependency;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.validation.IOpenLValidator;
import org.openl.validation.ValidationResult;

public abstract class TablesValidator implements IOpenLValidator {

    private void findAllTableSyntaxNodes(List<TableSyntaxNode> tableSyntaxNodes, IOpenClass openClass) {
        for (CompiledDependency compiledDependency : ((XlsModuleOpenClass) openClass).getDependencies()) {
            IOpenClass dependencyOpenClass = compiledDependency.getCompiledOpenClass().getOpenClass();
            findAllTableSyntaxNodes(tableSyntaxNodes, dependencyOpenClass);
        }

        XlsMetaInfo xlsMetaInfo = ((XlsModuleOpenClass) openClass).getXlsMetaInfo();
        TableSyntaxNode[] xlsTableSyntaxNodes = xlsMetaInfo.getXlsModuleNode().getXlsTableSyntaxNodes();
        for (TableSyntaxNode tableSyntaxNode : xlsTableSyntaxNodes) {
            tableSyntaxNodes.add(tableSyntaxNode);
        }
    }

    public ValidationResult validate(OpenL openl, IOpenClass openClass) {

        if (openClass instanceof XlsModuleOpenClass) {

            // Get all table syntax nodes of xls module.
            //
            List<TableSyntaxNode> tableSyntaxNodes = new ArrayList<TableSyntaxNode>();

            findAllTableSyntaxNodes(tableSyntaxNodes, openClass);

            return validateTables(openl, tableSyntaxNodes.toArray(new TableSyntaxNode[] {}), openClass);
        }

        // Skip validation if passed open class is not instance of
        // XlsModuleOpenClass.
        //
        return ValidationUtils.validationSuccess();
    }

    public abstract ValidationResult validateTables(OpenL openl,
            TableSyntaxNode[] tableSyntaxNodes,
            IOpenClass openClass);
}
