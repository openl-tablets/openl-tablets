package org.openl.studio.tags.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

import org.openl.studio.common.model.GenericView;

public class TagTypeDTO {

    @Getter
    @JsonView(GenericView.Full.class)
    @Parameter(description = "tags.tag-type.id.desc", required = true)
    @Setter
    private Long id;

    @Getter
    @JsonView({GenericView.CreateOrUpdate.class, GenericView.Full.class})
    @Parameter(description = "Tag type name", required = true)
    @Setter
    private String name;

    @Getter
    @JsonView({GenericView.CreateOrUpdate.class, GenericView.Full.class})
    @Setter
    private boolean extensible;

    @Getter
    @JsonView({GenericView.CreateOrUpdate.class, GenericView.Full.class})
    @Setter
    private boolean nullable;

    @Getter
    @JsonView(GenericView.Full.class)
    @Parameter(description = "Nested tags")
    @Setter
    private List<TagDTO> tags;
}
