package org.openl.studio.projects.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * A property a table may declare, and what a value for it looks like.
 *
 * @param name         property name
 * @param displayName  name a business user reads the property by
 * @param group        group the property is listed under
 * @param type         what a value is
 * @param multiple     whether the property takes several values, written separated by commas
 * @param dimensional  whether the engine dispatches on the property
 * @param defaultValue value the property stands for while a table declares none
 * @param pattern      regular expression a value must match, when the property states one
 * @param values       values the property accepts, empty unless the type is {@code enum}
 * @author Yury Molchan
 */
@Schema(description = "A property a table may declare")
@JsonInclude(JsonInclude.Include.ALWAYS)
public record PropertyDefinitionView(
        @Parameter(description = "Property name")
        String name,
        @Parameter(description = "Name a business user reads the property by")
        String displayName,
        @Parameter(description = "Group the property is listed under")
        String group,
        @Parameter(description = "Type of a value")
        @Schema(allowableValues = {"text", "date", "boolean", "enum"})
        String type,
        @Parameter(description = "Whether several values are accepted, separated by commas")
        boolean multiple,
        @Parameter(description = "Whether the engine dispatches on the property")
        boolean dimensional,
        @Parameter(description = "Value the property stands for while a table declares none")
        @Nullable String defaultValue,
        @Parameter(description = "Regular expression a value must match, when the property states one")
        @Nullable String pattern,
        @Parameter(description = "Values the property accepts, empty unless the type is an enum")
        List<PropertyValueView> values) {
}
