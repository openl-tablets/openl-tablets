package org.openl.studio.projects.model.files;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Base type for a file or folder in a mount (a project working copy or a repository subtree).
 *
 */
@Getter
@SuperBuilder
@JsonPropertyOrder({"path", "name", "type", "basePath", "hidden"})
@Schema(description = "Base resource representing a file or folder in the mount")
public abstract class FsNode {

    @Schema(description = "Mount-relative path (e.g. 'folder/rules.xlsx')")
    private final String path;

    @Schema(description = "Simple file or folder name")
    private final String name;

    @Schema(description = "Parent directory path (mount-relative)")
    private final String basePath;

    /**
     * Whether the screens of OpenL Studio hide the file or folder from a business user, unless the user asks to see
     * the hidden files: a technical file of a project, such as {@code rules.xml} or {@code pom.xml}. It is read and
     * written as any other file. Present only when {@code true}.
     */
    @Schema(description = "Whether OpenL Studio hides this technical project file, such as 'rules.xml' or 'pom.xml', "
            + "from business users unless they choose to show hidden files. It is read and written as any other "
            + "file. Present only when true.")
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private final boolean hidden;

    /**
     * Discriminator distinguishing a file from a folder: {@code "file"} or {@code "folder"}.
     */
    @Schema(description = "Resource type: 'file' or 'folder'", allowableValues = {"file", "folder"})
    public abstract String getType();
}
