package org.openl.studio.projects.model;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A field of a datatype, as the compiler resolved it.
 *
 * @param name       name of the field
 * @param type       type of the field as the rules write it, for example {@code Driver[]}
 * @param collection whether the field is an array
 * @author Yury Molchan
 */
@Schema(description = "A field of a datatype, as the compiler resolved it")
public record ProjectTypeFieldView(
        @Parameter(description = "Name of the field")
        String name,
        @Parameter(description = "Type of the field as the rules write it, for example Driver[]")
        String type,
        @Parameter(description = "Whether the field is an array")
        boolean collection) {
}
