package org.openl.studio.projects.model.project.status;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record ModuleMessageSource(
        @Parameter(description = "Module name (workbook file name without extension) the message originates from.")
        String name,

        @Parameter(description = """
                Identifier of the project the module belongs to, which is not always the project being compiled: \
                a message can be raised in a project it depends on. Absent when the project could not be named.""")
        String projectId,

        @Parameter(description = "Display name of the project the module belongs to.")
        String project,

        @Parameter(description = """
                Path in the project of the file of the module the message points to, such as a Markdown file of a \
                module folder that holds no table. Absent when the message points to no line of a file.""")
        @Nullable String path,

        @Parameter(description = "Line of the file the message points to, counted from 1.")
        @Nullable Integer line,

        @Parameter(description = """
                Places of the file the message is in, so an editor of the file marks them: the text of the line \
                of a Markdown file that breaks the format. Told only when the positions in the file are asked for, \
                as the compilation status tells them.""")
        @Nullable List<FilePlace> places
) implements MessageSource {
}
