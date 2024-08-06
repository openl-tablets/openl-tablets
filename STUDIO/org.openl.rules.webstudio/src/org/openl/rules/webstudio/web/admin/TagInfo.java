package org.openl.rules.webstudio.web.admin;

import org.openl.rules.security.standalone.persistence.Tag;

public class TagInfo {
    private final Tag tagEntity;
    private final boolean tagTypeExists;
    private final String typeName;
    private String name;

    public TagInfo(String typeName, String name, Tag tagEntity, boolean tagTypeExists) {
        this.tagEntity = tagEntity;
        this.typeName = typeName;
        this.name = name;
        this.tagTypeExists = tagTypeExists;
    }

    public Tag getTagEntity() {
        return tagEntity;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isNullable() {
        return tagEntity == null || tagEntity.getType().isNullable();
    }
    
    public boolean isExtensible() {
        return tagEntity != null && tagEntity.getType().isExtensible();
    }

    public boolean isTagTypeExists() {
        return tagTypeExists;
    }
}
