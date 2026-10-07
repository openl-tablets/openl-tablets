package org.openl.rules.rest.compile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import org.openl.message.Severity;

@Builder
@Schema(description = "Message description that contains information about compilation messages")
public record MessageDescription(long id, String summary, Severity severity) {

}
