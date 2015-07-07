package org.openl.rules.webstudio.web.test;

import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.types.IOpenField;

public class ParameterWithValueAndPreviewDeclaration extends ParameterWithValueDeclaration {
    private final IOpenField previewField;

    public ParameterWithValueAndPreviewDeclaration(ParameterWithValueDeclaration delegate, IOpenField previewField) {
        super(delegate.getName(), delegate.getValue(), delegate.getType());
        this.previewField = previewField;
    }

    public IOpenField getPreviewField() {
        return previewField;
    }
}
