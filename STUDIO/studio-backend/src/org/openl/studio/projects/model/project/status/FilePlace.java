package org.openl.studio.projects.model.project.status;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Where a message is in a text file, from a line and a column up to another, the end left out, so an editor of the
 * file marks it.
 *
 * <p>Lines count from 1. Columns count the characters of a line from 0, whatever line breaks the file is written
 * with.
 */
public record FilePlace(
        @Parameter(description = "Line the place begins on, the first line being 1")
        int line,

        @Parameter(description = "Column of the first character of the place, the first column being 0")
        int column,

        @Parameter(description = "Line the place ends on, the first line being 1")
        int endLine,

        @Parameter(description = "Column after the last character of the place, the first column being 0")
        int endColumn
) {
}
