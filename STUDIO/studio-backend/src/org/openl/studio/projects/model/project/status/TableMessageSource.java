package org.openl.studio.projects.model.project.status;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record TableMessageSource(
        @Parameter(description = """
                Identifier of the table the message originates from. \
                Matches the id used by the table editor and tree URLs.""")
        String id,

        @Parameter(description = "Display name of the table (the header cell text).")
        String name,

        @Parameter(description = "Module (workbook) the table belongs to.")
        String module,

        @Parameter(description = """
                Identifier of the project the table belongs to, which is not always the project being compiled: \
                a message can be raised in a project it depends on. Absent when the project could not be named.""")
        String projectId,

        @Parameter(description = "Display name of the project the table belongs to.")
        String project,

        @Parameter(description = """
                Address of the cell the message points to, in A1 notation. \
                May be null when the message is associated with the table as a whole.""")
        String cell,

        @Parameter(description = """
                First character of the cell's text the message is about, which a screen marks so the reader \
                finds it among the rest. Absent when the message is about no part of the cell in particular.""")
        Integer start,

        @Parameter(description = "Character of the cell's text after the last one the message is about.")
        Integer end,

        @Parameter(description = """
                Path in the project of the Markdown file the table is written in, for a message about a place \
                that breaks the format of the file: only the text of the file tells what is wrong, so a screen \
                sends the reader there rather than to the cell. Absent for any other message.""")
        @Nullable String path,

        @Parameter(description = "Line of that file the message points to, counted from 1.")
        @Nullable Integer line,

        @Parameter(description = """
                Places of that Markdown file the message is in, so an editor of the file marks them: the text of \
                the line that breaks the format. Told only when the positions in the file are asked for, as the \
                compilation status tells them.""")
        @Nullable List<FilePlace> places
) implements MessageSource {
}
