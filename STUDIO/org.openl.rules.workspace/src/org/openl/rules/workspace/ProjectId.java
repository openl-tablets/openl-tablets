package org.openl.rules.workspace;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jspecify.annotations.NonNull;

/**
 * The id a project is addressed by: 12 characters of URL-safe base64.
 *
 * <p>{@link ProjectIds} computes it from where the project is stored; a client never builds or decodes one. The id
 * is written to JSON as its text and read back from it.
 *
 * <p>Two ids are equal when their text is, case included. Any text makes an id: one no project has names no project.
 *
 * @param value the text of the id
 * @author Yury Molchan
 */
public record ProjectId(@JsonValue @NonNull String value) {

    public ProjectId {
        Objects.requireNonNull(value, "value must not be null");
    }

    /**
     * The id written as the given text, as a request or a JSON document names it.
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ProjectId of(String value) {
        return new ProjectId(value);
    }

    @Override
    public @NonNull String toString() {
        return value;
    }
}
