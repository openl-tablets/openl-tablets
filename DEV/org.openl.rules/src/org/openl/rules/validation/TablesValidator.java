package org.openl.rules.validation;

import org.openl.OpenL;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.validation.IOpenLValidator;
import org.openl.validation.ValidationResult;

public abstract class TablesValidator implements IOpenLValidator {

    public ValidationResult validate(OpenL openl, IOpenClass openClass) {

        if (openClass instanceof XlsModuleOpenClass) {

            // Get all table syntax nodes of xls module.
            //
            XlsMetaInfo xlsMetaInfo = ((XlsModuleOpenClass) openClass).getXlsMetaInfo();
            TableSyntaxNode[] tableSyntaxNodes = xlsMetaInfo.getXlsModuleNode().getXlsTableSyntaxNodes();

            return validateTables(openl, tableSyntaxNodes, openClass);
        }

        // Skip validation if passed open class is not instance of
        // XlsModuleOpenClass.
        //
        return ValidationUtils.validationSuccess();
    }

    public abstract ValidationResult validateTables(OpenL openl, TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass);
}
