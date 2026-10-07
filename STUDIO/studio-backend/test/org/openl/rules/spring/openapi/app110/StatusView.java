package org.openl.rules.spring.openapi.app110;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record StatusView(
        // Map keyed by an enum without @JsonProperty annotations on its constants — keys
        // should appear as the enum names (INFO/WARN/ERROR).
        LinkedHashMap<Severity, List<Message>> messages,

        // Map keyed by an enum with @JsonProperty annotations on its constants — keys
        // should appear with the @JsonProperty values (read/write).
        Map<Mode, String> labels,

        // Non-enum-keyed map — should keep the default additionalProperties shape.
        Map<String, Integer> counters
) {
}
