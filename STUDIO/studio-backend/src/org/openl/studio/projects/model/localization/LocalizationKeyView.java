package org.openl.studio.projects.model.localization;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A node of the tree of the messages: a key.
 *
 * @param key the key
 */
@Schema(description = "A key of the messages, as a node of their tree.")
public record LocalizationKeyView(
        @Parameter(description = "Key of the messages.")
        String key
) {
}
