package org.openl.rules.lang.xls.binding;

import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;

public class ConditionDefinition {

    private IParameterDeclaration[] parameterDeclarations;
    private String[] titles;

    private IOpenMethodHeader header;
    private CompositeMethod compositeMethod;

    public ConditionDefinition(String[] titles,
            IParameterDeclaration[] parameterDeclarations,
            IOpenMethodHeader header,
            CompositeMethod compositeMethod) {
        this.parameterDeclarations = parameterDeclarations;
        this.titles = titles;
        this.compositeMethod = compositeMethod;
        this.header = header;
    }

    public CompositeMethod getCompositeMethod() {
        return compositeMethod;
    }

    public int getNumberOfParameters() {
        return parameterDeclarations.length;
    }

    public IParameterDeclaration[] getParameterDeclarations() {
        return parameterDeclarations;
    }

    public String[] getTitles() {
        return titles;
    }

    public IOpenMethodHeader getHeader() {
        return header;
    }
}
