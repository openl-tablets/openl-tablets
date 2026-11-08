package org.openl.studio.projects.model.project.status;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

/**
 * The line of a localization file a compilation message is about, with the key and the language it names.
 *
 * <p>Its places are told one way at a time: as pieces of the text of the key, or as places of the file.
 *
 * <p>Another kind of file a message may be about gets a source of its own, beside this one.
 */
@Builder
public record LocalizationFileMessageSource(
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
        String language,

        @Parameter(description = """
                Pieces of the text of the key the message is about, in the order of the text, counted from its first \
                character. Told only when the positions in the text are asked for, and absent when the message is \
                about no part of the text in particular.""")
        List<TextSpot> spots,

        @Parameter(description = """
                Places of the file the message is in, so an editor of the file marks them: each piece of the text \
                where it is written, escapes and continued lines included, or the key for a message about its text \
                as a whole. Told only when the positions in the file are asked for, as the compilation status tells \
                them, and absent for a message about the whole file.""")
        List<FilePlace> places
) implements MessageSource {
}
