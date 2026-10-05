package org.openl.studio.projects.service.tables.theme;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Reads a colour a theme file sets: the colour of a font, of a fill or of a line.
 *
 * <p>The file writes a colour as {@code #rrggbb}, or by the name the theme gives it among its colours. A theme names
 * its colours once, so every part that takes a colour names it and changing the colour changes it everywhere.
 *
 * <p>A name the theme gives no colour is refused, and so is the theme. A colour written as {@code #rrggbb} is taken as
 * it is written: the look it is set in checks it.
 */
final class ThemeColourReader extends StdDeserializer<String> {

    /** The attribute that hands the reader the colours of the theme being read, by name. */
    static final String COLOURS = ThemeColourReader.class.getName();

    /** What starts a colour written as {@code #rrggbb} rather than by its name. */
    private static final String WRITTEN = "#";

    ThemeColourReader() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        var text = parser.getValueAsString();
        if (text == null) {
            return (String) context.handleUnexpectedToken(String.class, parser);
        }
        if (text.startsWith(WRITTEN)) {
            return text;
        }
        var colour = context.getAttribute(COLOURS) instanceof Map<?, ?> colours ? colours.get(text) : null;
        return colour instanceof String named ? named
                : context.reportInputMismatch(this,
                        "A colour is written as #rrggbb or by a name the theme gives it under colors: %s", text);
    }
}
