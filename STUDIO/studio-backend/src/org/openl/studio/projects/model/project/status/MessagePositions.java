package org.openl.studio.projects.model.project.status;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What the places a message about a localization file names are counted from.
 *
 * <p>A screen of the texts marks a piece of a text, an editor of the file a place of the file. The two differ wherever
 * the file escapes a character or continues the text on the next line.
 */
@Schema(description = "What the places of a message about a localization file are counted from")
public enum MessagePositions {

    @Schema(description = "The pieces of the text of the key, from its first character, told in spots")
    @JsonProperty("text")
    TEXT,

    @Schema(description = "The places of the file, by line and column, told in places")
    @JsonProperty("file")
    FILE
}
