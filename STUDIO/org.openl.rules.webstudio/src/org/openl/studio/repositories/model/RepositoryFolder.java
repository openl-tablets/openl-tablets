package org.openl.studio.repositories.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * A folder inside a non-flat design repository, offered as a candidate for importing an existing
 * project. {@code path} is the internal repository path; {@code mapped} is present only when the
 * folder (or a parent of it) is already imported as a project; {@code project} is present when the
 * folder holds an OpenL project (a {@code rules.xml} descriptor or Excel files) and can be imported.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RepositoryFolder(String name, String path, Boolean mapped, Boolean project) {
}
