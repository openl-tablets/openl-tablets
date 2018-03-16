package org.openl.types;

import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.syntax.ISyntaxNode;

public class FieldMetaInfo implements IMemberMetaInfo {

    private ISyntaxNode syntaxNode;
    private String displayName;
    private String sourceUrl;

    public FieldMetaInfo(String fieldType, String fieldName, ISyntaxNode syntaxNode, String sourceUrl) {
        this.displayName = fieldType + " " + fieldName;
        this.syntaxNode = syntaxNode;
        this.sourceUrl = sourceUrl;
    }

    @Override
    public BindingDependencies getDependencies() {
        return null;
    }

    @Override
    public Map<String, Object> getProperties() {
        return null;
    }

    @Override
    public ISyntaxNode getSyntaxNode() {
        return syntaxNode;
    }

    @Override
    public String getDisplayName(int mode) {
        return displayName;
    }

    @Override
    public String getSourceUrl() {
        return sourceUrl;
    }
}
