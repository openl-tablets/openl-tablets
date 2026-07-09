package org.openl.studio.repositories.model;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

import org.openl.rules.rest.model.UserInfoModel;

@Getter
@Setter
public class ProjectRevision {

    @Parameter(description = "Revision number", required = true)
    private String revisionNo;

    @Parameter(description = "Short revision number")
    private String shortRevisionNo;

    @Parameter(description = "Creation date-time", required = true)
    private ZonedDateTime createdAt;

    @Parameter(description = "Full comment", required = true)
    private String fullComment;

    @Parameter(description = "Author")
    @JsonView({UserInfoModel.View.Short.class})
    private UserInfoModel author;

    @Parameter(description = "If project was deleted or not.", required = true)
    private boolean deleted;

    @Parameter(description = "If current revision has changes in the project or not.", required = true)
    private boolean technicalRevision;

    @Parameter(description = "Comment parts. Always has 3 parts if present.")
    private List<String> commentParts;
}
