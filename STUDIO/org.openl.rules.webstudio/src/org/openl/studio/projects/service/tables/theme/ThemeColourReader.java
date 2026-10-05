package org.openl.studio.projects.service.tables.theme;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Reads a colour a theme file sets: the colour of a font, of a fill or of a line.
 *
 * <p>The file writes a colour as {@code #rrggbb}, as the palette of Excel names a theme colour, such as
 * {@code Blue, Accent 1, Lighter 60%}, or by the name the theme gives it among its colours. A theme names its colours
 * once, so every part that takes a colour names it and changing the colour changes it everywhere.
 *
 * <p>A colour written another way, a name the theme gives no colour, and a theme colour of a theme that writes no
 * theme colours are refused, and so is the theme.
 *
 * <p>Each colour read keeps the key of the file it is set at ({@link ThemeColour#key()}): the keys from the top of
 * the file down to the attribute, joined by dots, such as {@code spreadsheet.values.background}. The file is read with
 * its aliases and merge keys resolved, so a part an alias repeats is set at a key of its own.
 */
final class ThemeColourReader extends StdDeserializer<ThemeColour> {

    /** The attribute that hands the reader the colours of the theme being read, by name. */
    static final String COLOURS = ThemeColourReader.class.getName() + ".colours";

    /** The attribute that hands the reader the theme colours of Excel the theme makes its colours of. */
    static final String THEME_COLOURS = ThemeColourReader.class.getName() + ".themeColours";

    ThemeColourReader() {
        super(ThemeColour.class);
    }

    @Override
    public ThemeColour deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        var text = parser.getValueAsString();
        if (text == null) {
            return (ThemeColour) context.handleUnexpectedToken(ThemeColour.class, parser);
        }
        if (context.getAttribute(COLOURS) instanceof Map<?, ?> colours
                && colours.get(text) instanceof ThemeColour named) {
            return named.withKey(keyOf(parser));
        }
        ThemeColour written;
        try {
            var themeColours = context.getAttribute(THEME_COLOURS) instanceof ExcelThemeColours given ? given : null;
            written = ThemeColour.read(text, themeColours);
        } catch (IllegalArgumentException e) {
            return context.reportInputMismatch(this, "%s", e.getMessage());
        }
        return written != null ? written.withKey(keyOf(parser))
                : context.reportInputMismatch(this, "A colour is written as #rrggbb, as the palette of Excel names a "
                        + "theme colour, such as Blue, Accent 1, Lighter 60%%, or by a name the theme gives it under "
                        + "colors: %s", text);
    }

    /** The key of the file the value being read is set at: its keys from the top of the file down, joined by dots. */
    private static String keyOf(JsonParser parser) {
        var keys = new ArrayDeque<String>();
        for (var at = parser.getParsingContext(); at != null; at = at.getParent()) {
            if (at.getCurrentName() != null) {
                keys.addFirst(at.getCurrentName());
            }
        }
        return String.join(".", keys);
    }
}
