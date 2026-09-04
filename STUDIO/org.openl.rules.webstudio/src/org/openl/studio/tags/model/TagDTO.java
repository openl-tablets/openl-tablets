package org.openl.studio.tags.model;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

public class TagDTO {

    @Getter
    @Parameter(description = "tags.tag.id.desc")
    @Setter
    private Long id;

    @Getter
    @Parameter(description = "Tag name")
    @Setter
    private String name;

    @Getter
    @Parameter(description = "Linked tag type ID")
    @Setter
    private Long tagTypeId;
}
