package org.openl.studio.projects.model;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One value an enum property accepts.
 *
 * @param code  value written to the table
 * @param value value shown to the author
 * @author Yury Molchan
 */
@Schema(description = "One value an enum property accepts")
public record PropertyValueView(
        @Parameter(description = "Value written to the table")
        String code,
        @Parameter(description = "Value shown to the author")
        String value) {
}
