package org.openl.studio.projects.service.tables.theme;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.xls.PoiExcelHelper;

/**
 * One side of a cell border a theme draws.
 *
 * <p>The theme file writes a side either as the name of a line, such as {@code thin}, or with its colour:
 * {@code {style: thin, color: "#7f7f7f"}}, the colour written as {@code #rrggbb}, as the palette of Excel names a
 * theme colour, or by the name the theme gives it. A side without a colour is black.
 *
 * @param style the line
 * @param color the colour of the line, or {@code null} for black
 */
@JsonDeserialize(using = ThemeBorderLine.Reader.class)
public record ThemeBorderLine(ThemeLineStyle style, @Nullable ThemeColour color) {

    /** The colour of a line that names none. */
    private static final String BLACK = "#000000";

    /** The colour the line is drawn in, as the red, the green and the blue of it: black when it names none. */
    public short[] rgb() {
        return PoiExcelHelper.toRgb(Optional.ofNullable(color).map(ThemeColour::rgb).orElse(BLACK));
    }

    /** Whether the side is drawn with a line, rather than taken away. */
    boolean isLine() {
        return style != ThemeLineStyle.NONE;
    }

    /** A side written with its colour. */
    private record Written(@Nullable ThemeLineStyle style,
                           @JsonDeserialize(using = ThemeColourReader.class) @Nullable ThemeColour color) {
    }

    /** Reads a side written either way. */
    static final class Reader extends StdDeserializer<ThemeBorderLine> {

        Reader() {
            super(ThemeBorderLine.class);
        }

        @Override
        public ThemeBorderLine deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.currentToken() == JsonToken.VALUE_STRING) {
                return new ThemeBorderLine(context.readValue(parser, ThemeLineStyle.class), null);
            }
            var written = context.readValue(parser, Written.class);
            var style = written.style();
            if (style == null) {
                return context.reportInputMismatch(ThemeBorderLine.class, "A border side names no line style");
            }
            return new ThemeBorderLine(style, written.color());
        }
    }
}
