package org.openl.types;

import java.util.Map;

import lombok.Getter;

import org.openl.binding.BindingDependencies;
import org.openl.syntax.ISyntaxNode;

public class FieldMetaInfo implements IMemberMetaInfo {

    @Getter
    private final ISyntaxNode syntaxNode;
    private final String displayName;
    @Getter
    private final String sourceUrl;

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
    public String getDisplayName(int mode) {
        return displayName;
    }
}
