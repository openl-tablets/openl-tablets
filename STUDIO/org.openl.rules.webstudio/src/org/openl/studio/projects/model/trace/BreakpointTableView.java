package org.openl.studio.projects.model.trace;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A rule table that can be a breakpoint target.
 *
 * <p>Lets the user set a breakpoint on a table before it runs by searching for it by name. The
 * {@code uri} is the breakpoint key: execution suspends when a table with this URI is entered.
 *
 * @param uri  table source URI, used as the breakpoint key
 * @param name table display name, used to search
 * @param kind frame kind code shared with the UI
 */
@Schema(description = "trace.type.breakpoint-table.desc")
public record BreakpointTableView(
        @Schema(description = "trace.field.breakpoint-table.uri.desc")
        String uri,

        @Schema(description = "trace.field.breakpoint-table.name.desc")
        String name,

        @Schema(description = "trace.field.breakpoint-table.kind.desc")
        String kind
) {
}
