package org.openl.studio.projects.service.tables.theme;

import java.io.IOException;
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
            return named;
        }
        ThemeColour written;
        try {
            var themeColours = context.getAttribute(THEME_COLOURS) instanceof ExcelThemeColours given ? given : null;
            written = ThemeColour.read(text, themeColours);
        } catch (IllegalArgumentException e) {
            return context.reportInputMismatch(this, "%s", e.getMessage());
        }
        return written != null ? written
                : context.reportInputMismatch(this, "A colour is written as #rrggbb, as the palette of Excel names a "
                        + "theme colour, such as Blue, Accent 1, Lighter 60%%, or by a name the theme gives it under "
                        + "colors: %s", text);
    }
}
