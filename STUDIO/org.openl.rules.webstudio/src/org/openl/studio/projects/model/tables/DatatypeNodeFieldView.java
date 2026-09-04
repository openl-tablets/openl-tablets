package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A field a datatype declares, as the dependency graph reports it.
 *
 * <p>A field whose type is another datatype of the graph also names that datatype's node, so the data model can be
 * walked field by field. A field of a simple type carries its type only.
 *
 * @param ref        identifier of the datatype table the field refers to, absent when the type is not a datatype of
 *                   this graph
 * @param collection {@code true} when the field holds many values of that type, absent otherwise
 *
 * @author Vladyslav Pikus
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A field declared by a datatype")
public record DatatypeNodeFieldView(

        @Parameter(description = "Name of the field")
        String name,

        @Parameter(description = "Field type as declared, for example Driver[]")
        String type,

        @Parameter(description = "Identifier of the datatype table the field refers to, absent for a simple type")
        String ref,

        @Parameter(description = "true when the field holds many values of the type, absent otherwise")
        Boolean collection
) {
}
