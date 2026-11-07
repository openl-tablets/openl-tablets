package org.openl.studio.projects.model.project.status;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Polymorphic origin of a compilation message. Discriminated by the {@code type}
 * property so clients can render module-level and table-level locations differently
 * without unwrapping a union of optional fields.
 *
 * <p>The discriminator is emitted as the {@code type} property, the same convention
 * used by {@link org.openl.studio.projects.model.files.FsNode} so existing
 * client code can reuse the dispatch pattern.</p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ModuleMessageSource.class, name = "module"),
        @JsonSubTypes.Type(value = TableMessageSource.class, name = "table"),
        @JsonSubTypes.Type(value = FileMessageSource.class, name = "file")
})
@Schema(description = """
        Location of a compilation message: a module (workbook), a specific table inside it, or a line of another \
        file of the project, such as a localization file.""")
public sealed interface MessageSource permits ModuleMessageSource, TableMessageSource, FileMessageSource {
}
