package org.openl.rules.webstudio.web.admin;

import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;

public class TagInfo {
    private final Tag tagEntity;
    private final TagType tagType;
    private final String typeName;
    private String name;

    public TagInfo(String typeName, String name, Tag tagEntity, TagType tagType) {
        this.tagEntity = tagEntity;
        this.tagType = tagType;
        this.typeName = typeName;
        this.name = name;
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
        return tagType == null || tagType.isNullable();
    }
    
    public boolean isExtensible() {
        return tagType != null && tagType.isExtensible();
    }

    public boolean isTagTypeExists() {
        return tagType != null;
    }
}
