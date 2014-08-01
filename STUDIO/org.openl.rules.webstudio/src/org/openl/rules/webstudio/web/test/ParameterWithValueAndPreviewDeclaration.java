package org.openl.rules.webstudio.web.test;

import org.openl.rules.testmethod.ParameterWithValueDeclaration;

public class ParameterWithValueAndPreviewDeclaration extends ParameterWithValueDeclaration {
    private final Object preview;

    public ParameterWithValueAndPreviewDeclaration(ParameterWithValueDeclaration delegate, Object preview) {
        super(delegate.getName(), delegate.getValue(), delegate.getType(), delegate.getDirection());
        this.preview = preview;
    }

    public Object getPreview() {
        return preview;
    }
}
