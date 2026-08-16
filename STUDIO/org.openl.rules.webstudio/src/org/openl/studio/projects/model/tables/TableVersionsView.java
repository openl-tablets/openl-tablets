package org.openl.studio.projects.model.tables;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * The versions of one table: the one it stands for, the one offered to a new version of it, and the ones taken.
 * <p>
 * Versions are ordered by three numbers — major, minor and variant — and only one version of a table is active at a
 * time. A copy that keeps the table's name and answers the same requests is another version of it, so it needs a
 * version that no version of the table already carries.
 *
 * @param current the version the table stands for; {@code 0.0.1} while it declares none
 * @param next    the first free version after the current one
 * @param taken   versions already carried by the table's versions, the current one included
 * @author Vladyslav Pikus
 */
public record TableVersionsView(
        @Parameter(description = "Version the table stands for; 0.0.1 while it declares none")
        String current,

        @Parameter(description = "First free version after the current one")
        String next,

        @Parameter(description = "Versions already carried by the table's versions, the current one included")
        List<String> taken
) {
}
