package org.openl.rules.validation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.openl.OpenL;
import org.openl.binding.impl.MethodUsagesSearcher.MethodUsage;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationStatus;
import org.openl.validation.ValidationUtils;

/**
 * Checks that the auxiliary methods for overloaded rules(<method name>$<index>)
 * are not used in rules(except of internal OpenL tables such as dispatcher
 * table).
 * 
 * @author PUdalau
 */
public class AuxiliaryMethodsValidator extends TablesValidator {

    public static final String ERROR_MESSAGE = "Auxiliary method usages are not allowed in user tables.";

    @Override
    public ValidationResult validateTables(OpenL openl, TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass) {
        ValidationResult validationResult = null;

        for (TableSyntaxNode tsn : tableSyntaxNodes) {
            // only dispatcher table can use auxiliary methods
            if (!isDimensionPropertiesDispatcherTable(tsn)) {
                List<OpenLMessage> messages = checkAuxiliaryMethodUsages(tsn);
                if (CollectionUtils.isNotEmpty(messages)) {
                    if (validationResult == null) {
                        validationResult = new ValidationResult(ValidationStatus.FAIL);
                    }
                    for (OpenLMessage message : messages) {
                        ValidationUtils.addValidationMessage(validationResult, message);
                    }
                }
            }
        }
        if (validationResult != null) {
            return validationResult;
        }
        return ValidationUtils.validationSuccess();
    }

    private List<OpenLMessage> checkAuxiliaryMethodUsages(TableSyntaxNode tsn) {
        List<OpenLMessage> messages = new ArrayList<OpenLMessage>();
        IGridTable gridTable = tsn.getGridTable();
        for (int column = 0; column < gridTable.getWidth(); column++) {
            for (int row = 0; row < gridTable.getHeight(); row++) {
                CellMetaInfo cellMetaInfo = gridTable.getCell(column, row).getMetaInfo();
                if (cellMetaInfo != null && cellMetaInfo.hasMethodUsagesInCell()) {
                    for (MethodUsage usedMethodDescription : cellMetaInfo.getUsedMethods()) {
                        if (XlsModuleOpenClass.isAuxiliaryMethod(usedMethodDescription.getMethod())) {
                            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(ERROR_MESSAGE,
                                new GridCellSourceCodeModule(gridTable, column, row, null));
                            tsn.addError(error);
                            messages.add(new OpenLErrorMessage(error));
                        }
                    }
                }
            }
        }
        return messages;
    }

    /**
     * TODO move to helper (dimpropsvalidator) Check if {@link TableSyntaxNode}
     * represents generated dispatcher decision table for dimension properties.
     * 
     * @param tsn {@link TableSyntaxNode}
     * @return true if {@link TableSyntaxNode} represents generated dispatcher
     *         decision table for dimension properties.
     */
    private boolean isDimensionPropertiesDispatcherTable(TableSyntaxNode tsn) {
        return tsn.getDisplayName() != null && tsn.getDisplayName()
            .contains(DispatcherTablesBuilder.DEFAULT_DISPATCHER_TABLE_NAME) && tsn.getMember() instanceof DecisionTable;
    }

}
