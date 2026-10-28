package org.openl.studio.projects.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * A type the rules of a project may name, as the compiler resolved it.
 *
 * <p>The types of the projects it depends on are among them. A datatype lists every field it has, the inherited ones
 * first, whichever way its table is laid out. A vocabulary lists the values it accepts instead.
 *
 * <p>A type whose table did not compile is not among them.
 *
 * @param name      name the rules refer to the type by
 * @param kind      whether the type declares fields or values
 * @param project   project declaring the type, absent when it cannot be told
 * @param projectId identifier of that project, absent when it cannot be told
 * @param module    module declaring the type, absent when it cannot be told
 * @param fields    fields of a datatype, empty for a vocabulary
 * @param values    values of a vocabulary, as text, empty for a datatype
 * @author Yury Molchan
 */
@Schema(description = "A type the rules of a project may name, as the compiler resolved it")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectTypeView(
        @Parameter(description = "Name the rules refer to the type by")
        String name,
        @Parameter(description = "Whether the type declares fields or values")
        ProjectTypeKind kind,
        @Parameter(description = "Project declaring the type: this project or one it depends on")
        @Nullable String project,
        @Parameter(description = "Identifier of that project, as the Projects API addresses it")
        @Nullable String projectId,
        @Parameter(description = "Module declaring the type")
        @Nullable String module,
        @Parameter(description = "Fields of a datatype, the inherited ones first; empty for a vocabulary")
        List<ProjectTypeFieldView> fields,
        @Parameter(description = "Values a vocabulary accepts, as text; empty for a datatype")
        List<String> values) {
}
