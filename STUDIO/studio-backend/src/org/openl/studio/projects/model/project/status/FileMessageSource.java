package org.openl.studio.projects.model.project.status;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

@Builder
public record FileMessageSource(
        @Parameter(description = """
                Path of the file the message is about, relative to the project folder. \
                The address of the file when it lies outside the folder or in an archive.""")
        String path,

        @Parameter(description = "Line of the file the message points to, the first line being 1.")
        Integer line,

        @Parameter(description = """
                Key of the localization file the message is about. \
                Absent when the message is about the file as a whole.""")
        String key,

        @Parameter(description = """
                Language of the localization file: the language, country and variant of its name joined by \
                underscores, empty for the default file.""")
        String language
) implements MessageSource {
}
