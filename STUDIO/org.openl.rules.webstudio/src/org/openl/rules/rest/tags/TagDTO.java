package org.openl.rules.rest.tags;

import io.swagger.v3.oas.annotations.media.Schema;

public class TagDTO {

    @Schema(description = "tags.tag.id.desc")
    private Long id;

    @Schema(description = "Tag name")
    private String name;

    @Schema(description = "Linked tag type ID")
    private Long tagTypeId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTagTypeId() {
        return tagTypeId;
    }

    public void setTagTypeId(Long tagTypeId) {
        this.tagTypeId = tagTypeId;
    }
}
