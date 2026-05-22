package org.openl.studio.projects.model.project.status;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

import org.openl.rules.rest.compile.MessageDescription;

/**
 * Compilation message enriched with its origin and stack-trace availability.
 *
 * <p>The {@link MessageDescription} fields are flattened into the parent object via
 * {@link JsonUnwrapped} so existing clients that only consume {@code id}, {@code summary}
 * and {@code severity} keep working without any unwrapping logic.</p>
 */
@Builder
public record DetailedMessageDescription(
        @JsonUnwrapped
        @Parameter(description = "Compilation message identity, summary and severity.")
        MessageDescription source,

        @Parameter(description = "Origin of the message (module or table). Absent when the message has no source location.")
        MessageSource location,

        @Parameter(description = "True when the message carries an attached stack trace. Omitted when no stack trace is available (e.g. warnings).")
        Boolean stacktrace
) {
}
