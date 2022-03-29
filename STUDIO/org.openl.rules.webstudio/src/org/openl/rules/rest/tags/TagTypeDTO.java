package org.openl.rules.rest.tags;

import java.util.List;

import org.openl.rules.rest.model.GenericView;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.v3.oas.annotations.media.Schema;

public class TagTypeDTO {

    @JsonView(GenericView.Full.class)
    @Schema(description = "tags.tag-type.id.desc", required = true)
    private Long id;

    @JsonView({ GenericView.CreateOrUpdate.class, GenericView.Full.class })
    @Schema(description = "Tag type name", required = true)
    private String name;

    @JsonView({ GenericView.CreateOrUpdate.class, GenericView.Full.class })
    private boolean extensible;

    @JsonView({ GenericView.CreateOrUpdate.class, GenericView.Full.class })
    private boolean nullable;

    @JsonView(GenericView.Full.class)
    @Schema(description = "Nested tags")
    private List<TagDTO> tags;

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

    public boolean isExtensible() {
        return extensible;
    }

    public void setExtensible(boolean extensible) {
        this.extensible = extensible;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public List<TagDTO> getTags() {
        return tags;
    }

    public void setTags(List<TagDTO> tags) {
        this.tags = tags;
    }
}
