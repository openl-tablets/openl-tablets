package org.openl.studio.projects.model.project.status;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.rules.repository.api.FileChange;

@Schema(description = "What became of a file or a project: of a pending change, against the design revision; of a merge, against the branch before it")
public enum ChangeType {

    @Schema(description = "Present after the change and absent before it")
    @JsonProperty("added")
    ADDED,

    @Schema(description = "Present before and after the change, with other content")
    @JsonProperty("modified")
    MODIFIED,

    @Schema(description = "Present before the change and absent after it")
    @JsonProperty("deleted")
    DELETED;

    /** What a repository tells became of a file. */
    public static ChangeType of(FileChange.Kind kind) {
        return switch (kind) {
            case ADDED -> ADDED;
            case MODIFIED -> MODIFIED;
            case DELETED -> DELETED;
        };
    }
}
