package org.openl.studio.projects.service.tables.theme;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import org.openl.rules.table.IOpenLTable;

/**
 * The table theme of OpenL Studio: the {@code table-theme.yaml} file on its classpath, which follows the formatting
 * standard of OpenL tables.
 *
 * <p>The file is read with its YAML anchors, aliases and merge keys resolved, so one part of the theme can extend or
 * repeat another. The file can name its colours once, under {@code colors}, and set a colour by its name wherever a
 * part takes one. Every colour is written as the palette of Excel names a theme colour, such as
 * {@code Blue, Accent 1, Lighter 60%}.
 *
 * <p>A theme file that cannot be read, that names an attribute a theme does not know, a colour it gives no name, or a
 * colour written another way is refused, so a mistyped attribute is never silently ignored: OpenL Studio does not
 * start with a theme it cannot read.
 */
@Slf4j
@Service
public class TableThemeService {

    /** Where the theme lives on the classpath. */
    private static final String LOCATION = "table-theme.yaml";

    /** The key of a theme file that gives its colours their names. */
    private static final String COLOURS = "colors";

    private static final TypeReference<Map<String, String>> NAMED_COLOURS = new TypeReference<>() {
    };

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            // A size such as 10.5 is refused rather than cut down to 10, which the file does not say.
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .build();

    private final TableTheme theme;

    @Autowired
    public TableThemeService() {
        this(new ClassPathResource(LOCATION));
    }

    /**
     * The theme of a theme file other than the one OpenL Studio ships, such as one a test writes.
     *
     * @param file the theme file
     * @throws IllegalStateException when the file is not a theme, as {@link #read} tells
     */
    TableThemeService(Resource file) {
        theme = read(file);
    }

    /**
     * Whether the theme styles a table. The theme styles every table of the kinds {@link ThemeLayouts#styles} names.
     *
     * @param table the table
     * @return {@code true} for a table of a kind the theme styles
     */
    public boolean styles(IOpenLTable table) {
        return ThemeLayouts.styles(table);
    }

    /**
     * The look the theme gives each cell of a table, for a screen to draw it.
     *
     * <p>Every table a screen shows is drawn with the theme, so a table the theme cannot be laid out over is drawn plain
     * rather than not drawn at all; the failure is logged.
     *
     * @param table the table to theme
     * @return the look of each cell, or {@code null} for a table of a kind the theme does not style and for one it
     *         cannot be laid out over
     */
    public @Nullable ThemedTable layoutOf(IOpenLTable table) {
        try {
            return ThemeLayouts.of(table, table.getGridTable(), theme);
        } catch (RuntimeException failed) {
            log.warn("The table theme cannot be laid out over the table '{}'; it is drawn plain.", table.getName(),
                    failed);
            return null;
        }
    }

    /**
     * A writer of the theme into workbooks, for one batch of tables.
     *
     * @return the writer
     */
    public ThemeExcelWriter writer() {
        return new ThemeExcelWriter(theme);
    }

    /** The theme. */
    TableTheme theme() {
        return theme;
    }

    /**
     * Reads the theme a file describes.
     *
     * @param file the theme file
     * @return the theme
     * @throws IllegalStateException when the file is empty or not a theme, names an attribute a theme does not know or
     *                               a colour it does not name, writes a colour another way than the palette of Excel
     *                               names a theme colour, or writes a key twice
     * @throws UncheckedIOException  when the file cannot be read
     */
    static TableTheme read(Resource file) {
        var options = new LoaderOptions();
        // A key written twice is a mistake: the second would silently replace the first.
        options.setAllowDuplicateKeys(false);
        TableTheme theme;
        try (var in = file.getInputStream()) {
            theme = bind(new Yaml(new SafeConstructor(options)).load(in));
        } catch (IOException e) {
            throw new UncheckedIOException("The table theme cannot be read: " + file.getDescription(), e);
        } catch (RuntimeException e) {
            throw new IllegalStateException("The table theme cannot be read: " + file.getDescription(), e);
        }
        if (theme == null) {
            throw new IllegalStateException("The table theme is empty: " + file.getDescription());
        }
        return theme;
    }

    /**
     * The theme the YAML of a file describes, each colour it names by name read as the colour it gives the name.
     *
     * @param tree the YAML of the file, read with its anchors, aliases and merge keys resolved
     * @return the theme, or {@code null} for an empty file
     * @throws IllegalArgumentException when the theme writes one of its colours another way than the palette of Excel
     *                                  names a theme colour, or the file is not a theme
     */
    private static @Nullable TableTheme bind(@Nullable Object tree) {
        if (tree == null) {
            return null;
        }
        // The colours are a key of the file, not of the theme it describes: each part that takes a colour has it.
        var named = tree instanceof Map<?, ?> keys ? keys.remove(COLOURS) : null;
        var reader = MAPPER.readerFor(TableTheme.class)
                .withAttribute(ThemeColourReader.COLOURS, coloursOf(MAPPER.convertValue(named, NAMED_COLOURS)));
        try {
            return reader.readValue(MAPPER.<JsonNode>valueToTree(tree));
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /** The colours a theme names, each read as the palette of Excel names a theme colour. */
    private static Map<String, ThemeColour> coloursOf(@Nullable Map<String, String> named) {
        var colours = new HashMap<String, ThemeColour>();
        if (named != null) {
            named.forEach((name, text) -> colours.put(name, Optional.ofNullable(text)
                    .map(ThemeColour::read)
                    .orElseThrow(() -> new IllegalArgumentException("A colour the theme names is written as the palette "
                            + "of Excel names a theme colour: " + name + ": " + text))));
        }
        return colours;
    }
}
