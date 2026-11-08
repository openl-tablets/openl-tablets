package org.openl.studio.projects.model.project.status;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * A piece of a text a message is about, which a screen marks so the reader finds it among the rest.
 *
 * @param start index of the first character of the piece
 * @param end   index after the last character of the piece
 */
public record TextSpot(
        @Parameter(description = "Index of the first character of the piece of the text")
        int start,

        @Parameter(description = "Index after the last character of the piece of the text")
        int end
) {
}
