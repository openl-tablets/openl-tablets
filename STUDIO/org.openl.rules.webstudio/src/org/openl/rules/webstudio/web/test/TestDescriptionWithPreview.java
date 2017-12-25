package org.openl.rules.webstudio.web.test;

import org.openl.rules.data.ColumnDescriptor;
import org.openl.rules.data.ForeignKeyColumnDescriptor;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestDescription;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

public class TestDescriptionWithPreview extends TestDescription {
    private ParameterWithValueAndPreviewDeclaration[] paramsWithPreview;

    public TestDescriptionWithPreview(TestDescription delegate) {
        super(delegate.getTestedMethod(), delegate.getTestObject(), delegate.getFields(), delegate.getColumnDescriptors());
        setIndex(delegate.getIndex());
    }

    @Override
    public ParameterWithValueDeclaration[] getExecutionParams() {
        if (paramsWithPreview == null) {
            paramsWithPreview = initParamsWithPreview();
        }

        return paramsWithPreview;
    }

    private ParameterWithValueAndPreviewDeclaration[] initParamsWithPreview() {
        ParameterWithValueAndPreviewDeclaration[] paramsWithPreview;

        ParameterWithValueDeclaration[] executionParams = super.getExecutionParams();
        paramsWithPreview = new ParameterWithValueAndPreviewDeclaration[executionParams.length];
        for (int i = 0; i < executionParams.length; i++) {
            ParameterWithValueDeclaration param = executionParams[i];
            IOpenField previewField = getPreviewField(param.getName(), param.getType(), param.getValue());
            paramsWithPreview[i] = new ParameterWithValueAndPreviewDeclaration(param, previewField);
        }

        return paramsWithPreview;
    }

    private IOpenField getPreviewField(String paramName, IOpenClass type, Object value) {
        if (value == null) {
            return null;
        }
        IOpenField foreignKeyField = null;
        ColumnDescriptor[] columnDescriptors = getColumnDescriptors();
        if (columnDescriptors != null) {
            for (ColumnDescriptor columnDescriptor : columnDescriptors) {
                if (columnDescriptor == null) {
                    continue;
                }
                IdentifierNode[] fieldChainTokens = columnDescriptor.getFieldChainTokens();
                if (fieldChainTokens.length > 0 && fieldChainTokens[0].getIdentifier().equals(paramName)) {
                    // Found first column descriptor for needed parameter
                    if (columnDescriptor.isReference() && columnDescriptor instanceof ForeignKeyColumnDescriptor) {
                        // Foreign key to a data described in the Data Table
                        ForeignKeyColumnDescriptor descriptor = (ForeignKeyColumnDescriptor) columnDescriptor;
                        foreignKeyField = descriptor.getForeignKeyField(type);
                    } else {
                        // Test data is described in the current Test Table
                        if (fieldChainTokens.length > 1) {
                            // The field of a complex bean
                            IdentifierNode fieldName = fieldChainTokens[fieldChainTokens.length - 1];
                            foreignKeyField = type.getField(fieldName.getIdentifier());
                        }
                    }
                    break;
                }
            }
        }
        if (foreignKeyField == null) {
            // Couldn't find foreign key field in foreign Data Table or current Test Table - fallback to index field
            foreignKeyField = type.getIndexField();
        }

        return foreignKeyField;
    }
}
