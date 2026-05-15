package org.openl.studio.projects.rest.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Parameter(description = "Project identity. Either a project ID or a project name. If a name is provided and multiple projects share it across repositories, a conflict error is returned with the list of candidate IDs.", in = ParameterIn.PATH, required = true, schema = @Schema(implementation = String.class))
public @interface ProjectId {

}
