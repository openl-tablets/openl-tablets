package org.openl.studio.projects.model.trace;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A rule table that can be a breakpoint target, identified by name.
 *
 * <p>Lets the user set a breakpoint on a table before it runs by searching for it by name. The
 * {@code name} is the breakpoint key: execution suspends when any table with this name is entered, so a
 * breakpoint on an overloaded or dimensional rule stops on every version of it.
 *
 * @param name table name, the breakpoint key and the search term
 * @param kind frame kind code shared with the UI
 */
@Schema(description = "trace.type.breakpoint-table.desc")
public record BreakpointTableView(
        @Schema(description = "trace.field.breakpoint-table.name.desc")
        String name,

        @Schema(description = "trace.field.breakpoint-table.kind.desc")
        String kind
) {
}
