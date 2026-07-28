package org.openl.studio.projects.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A property a table may declare, and what a value for it looks like.
 *
 * @param name     property name
 * @param type     what a value is
 * @param multiple whether the property takes several values, written separated by commas
 * @param values   values the property accepts, empty unless the type is {@code enum}
 * @author Yury Molchan
 */
@Schema(description = "A property a table may declare")
@JsonInclude(JsonInclude.Include.ALWAYS)
public record PropertyDefinitionView(
        @Parameter(description = "Property name")
        String name,
        @Parameter(description = "Type of a value")
        @Schema(allowableValues = {"text", "date", "boolean", "enum"})
        String type,
        @Parameter(description = "Whether several values are accepted, separated by commas")
        boolean multiple,
        @Parameter(description = "Values the property accepts, empty unless the type is an enum")
        List<PropertyValueView> values) {
}
