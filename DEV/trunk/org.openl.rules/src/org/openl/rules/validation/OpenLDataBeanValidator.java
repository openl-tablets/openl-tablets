package org.openl.rules.validation;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.OpenL;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.rules.data.ColumnDescriptor;
import org.openl.rules.data.DataOpenField;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.validation.IOpenLValidator;
import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationStatus;
import org.openl.validation.ValidationUtils;

/**
 * The base implementation of {@link IOpenLValidator} which can be used to
 * validate data tables.
 * 
 * @param <T> bean type
 */
public abstract class OpenLDataBeanValidator<T> implements IOpenLValidator {

    private Class<?> genericType;

    public abstract BeanValidationResult validateBean(T bean);

    public ValidationResult validate(OpenL openl, IOpenClass openClass) {

        if (openClass instanceof XlsModuleOpenClass) {
            // Get all table syntax nodes of xls module.
            XlsMetaInfo xlsMetaInfo = ((XlsModuleOpenClass) openClass).getXlsMetaInfo();
            TableSyntaxNode[] tableSyntaxNodes = xlsMetaInfo.getXlsModuleNode().getXlsTableSyntaxNodes();

            // Find data tables with specified component type.
            TableSyntaxNode[] fields = findFieldMembers(tableSyntaxNodes, getGenericType());

            List<OpenLMessage> validationMessages = new ArrayList<OpenLMessage>();

            for (TableSyntaxNode node : fields) {
                ValidationResult result = validateDataNode(node);

                if (!ValidationStatus.SUCCESS.equals(result.getStatus())) {
                    validationMessages.addAll(result.getMessages());
                }
            }

            if (validationMessages.size() > 0) {
                return new ValidationResult(ValidationStatus.FAIL, validationMessages);
            }

            return ValidationUtils.validationSuccess();
        }

        // Skip validation if passed open class is not instance of
        // XlsModuleOpenClass.
        //
        return ValidationUtils.validationSuccess();
    }

    private ValidationResult validateDataNode(TableSyntaxNode node) {
        @SuppressWarnings("unchecked")
        T[] data = ((T[]) ((DataOpenField) node.getMember()).getData());

        ValidationResult failedValidationResult = new ValidationResult(ValidationStatus.FAIL);

        for (int i = 0; i < data.length; i++) {
            T bean = data[i];
            BeanValidationResult result = validateBean(bean);

            if (!ValidationStatus.SUCCESS.equals(result.getStatus())) {
                // Link bean error with source.
                List<SyntaxNodeException> errors = prepareErrorsList(result, i, node);

                for (SyntaxNodeException error : errors) {
                    // Add error to syntax node.
                    node.addError(error);
                    // Update list of error messages.
                    failedValidationResult.getMessages().add(new OpenLErrorMessage(error));
                }
            }
        }

        if (failedValidationResult.hasMessages()) {
            return failedValidationResult;
        }

        return ValidationUtils.validationSuccess();
    }

    private List<SyntaxNodeException> prepareErrorsList(BeanValidationResult result, int beanIndex,
        TableSyntaxNode syntaxNode) {
        // Create field name - column index pairs to search column index by
        // field name.
        Map<String, Integer> descriptorsMap = createDescriptorsMap(((DataOpenField) syntaxNode.getMember()).getTable()
            .getDataModel().getDescriptor());

        List<SyntaxNodeException> errors = new ArrayList<SyntaxNodeException>();

        for (PropertyConstraintViolation violation : result.getPropertyConstraintViolations()) {
            String invalidPropName = violation.getPropertyName();
            // Get invalid cell.
            ILogicalTable cell = syntaxNode.getTableBody().getRows(2)
                .getSubtable(descriptorsMap.get(invalidPropName), beanIndex, 1, 1);
            // Create syntax node error instance.
            SyntaxNodeException ex = SyntaxNodeExceptionUtils.createError(violation.getMessage(),
                new GridCellSourceCodeModule(cell.getSource()));

            errors.add(ex);
        }

        return errors;
    }

    private Map<String, Integer> createDescriptorsMap(ColumnDescriptor[] descriptors) {
        Map<String, Integer> map = new HashMap<String, Integer>();

        for (int i = 0; i < descriptors.length; i++) {
            map.put(descriptors[i].getName(), i);
        }

        return map;
    }

    private TableSyntaxNode[] findFieldMembers(TableSyntaxNode[] tableSyntaxNodes, Class<?> fieldComponentType) {
        List<TableSyntaxNode> fields = new ArrayList<TableSyntaxNode>();

        for (TableSyntaxNode node : tableSyntaxNodes) {
            if (node.getMember() instanceof DataOpenField) {
                if (node.getMember().getType().getInstanceClass().getComponentType() == fieldComponentType) {
                    fields.add(node);
                }
            }
        }

        return fields.toArray(new TableSyntaxNode[fields.size()]);
    }

    /**
     * Gets type of generic parameter.
     * 
     * @return class object
     */
    private Class<?> getGenericType() {

        if (this.genericType == null) {
            ParameterizedType superclass = (ParameterizedType) getClass().getGenericSuperclass();
            this.genericType = (Class<?>) ((ParameterizedType) superclass).getActualTypeArguments()[0];
        }

        return this.genericType;
    }
}