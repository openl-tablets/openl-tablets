package org.openl.studio.projects.service.tables.theme;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.TableThemeView;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

/**
 * The table themes OpenL Studio offers: every {@code table-themes/*.yaml} file on its classpath.
 *
 * <p>A theme is known by the name of its file without the extension, and shown by the name the file declares.
 * The file is read with its YAML anchors, aliases and merge keys resolved, so one part of a theme can extend or
 * repeat another. The file can name its colours once, under {@code colors}, and set a colour by its name wherever a
 * part takes one.
 *
 * <p>A theme file that cannot be read, that declares no name, that names an attribute a theme does not know, or a
 * colour it gives no name, is not offered. Studio starts with the other themes and logs why the file was refused, so
 * a mistyped attribute is never silently ignored.
 */
@Slf4j
@Service
public class TableThemeService {

    /** Where the themes live on the classpath. */
    static final String LOCATION = "classpath*:table-themes/*.yaml";

    /** The key of a theme file that gives its colours their names. */
    private static final String COLOURS = "colors";

    private static final TypeReference<Map<String, String>> NAMED_COLOURS = new TypeReference<>() {
    };

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            // A size such as 10.5 is refused rather than cut down to 10, which the file does not say.
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .build();

    private final Map<String, TableTheme> themes;

    @Autowired
    public TableThemeService() {
        this(LOCATION);
    }

    TableThemeService(String location) {
        Resource[] files;
        try {
            files = new PathMatchingResourcePatternResolver().getResources(location);
        } catch (IOException e) {
            throw new UncheckedIOException("The table themes cannot be listed: " + location, e);
        }
        var read = new LinkedHashMap<String, TableTheme>();
        for (var file : files) {
            var id = FileUtils.getBaseName(Objects.requireNonNull(file.getFilename()));
            var theme = offered(file);
            if (theme != null && read.putIfAbsent(id, theme) != null) {
                log.warn("The table theme '{}' is defined twice; the one met first is kept: {}", id,
                        file.getDescription());
            }
        }
        themes = sortedByName(read);
    }

    /**
     * The themes OpenL Studio offers, by name.
     *
     * @return the themes, each with the identifier it is asked for by and the name it is shown by
     */
    public List<TableThemeView> getThemes() {
        return themes.entrySet().stream().map(TableThemeService::viewOf).toList();
    }

    /**
     * The themes that can be drawn over a table or written into it, by name.
     *
     * @param table the table
     * @return every theme for a table the themes style; none for a table of any other kind
     */
    public List<TableThemeView> getThemes(IOpenLTable table) {
        return styles(table) ? getThemes() : List.of();
    }

    /**
     * Whether the themes style a table. Every theme styles every table of the kinds {@link ThemeLayouts#styles}
     * names, so they look alike in one theme.
     *
     * @param table the table
     * @return {@code true} for a table of a kind the themes style
     */
    public boolean styles(IOpenLTable table) {
        return ThemeLayouts.styles(table);
    }

    private static TableThemeView viewOf(Map.Entry<String, TableTheme> theme) {
        return new TableThemeView(theme.getKey(), theme.getValue().name());
    }

    /**
     * The look a theme gives each cell of a table, for a screen to draw it.
     *
     * @param table   the table to theme
     * @param themeId the theme, by its identifier
     * @return the look of each cell, or {@code null} for a table of a kind no theme styles
     * @throws BadRequestException when no theme has the identifier
     */
    public @Nullable ThemedTable layoutOf(IOpenLTable table, String themeId) {
        return ThemeLayouts.of(table, table.getGridTable(), theme(themeId), TableMoves.NONE);
    }

    /**
     * A writer of a theme into workbooks, for one batch of tables.
     *
     * @param themeId the theme, by its identifier
     * @return the writer
     * @throws BadRequestException when no theme has the identifier
     */
    public ThemeExcelWriter writer(String themeId) {
        return new ThemeExcelWriter(theme(themeId));
    }

    /** The theme of the identifier, which a request named. */
    TableTheme theme(String themeId) {
        var theme = themes.get(themeId);
        if (theme == null) {
            throw new BadRequestException("table.theme.unknown.message", new Object[]{themeId});
        }
        return theme;
    }

    /** The theme a file describes, or {@code null} for a file that is refused. */
    private static @Nullable TableTheme offered(Resource file) {
        try {
            return read(file);
        } catch (RuntimeException e) {
            log.error("The table theme is not offered: it cannot be read.", e);
            return null;
        }
    }

    /**
     * Reads the theme a file describes.
     *
     * @param file the theme file
     * @return the theme
     * @throws IllegalStateException when the file is not a theme, names an attribute a theme does not know or a
     *                               colour it does not name, writes a key twice or declares no name
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
        if (theme == null || StringUtils.isBlank(theme.name())) {
            throw new IllegalStateException("The table theme declares no name: " + file.getDescription());
        }
        return theme;
    }

    /**
     * The theme the YAML of a file describes, each colour it names by name read as the colour it gives the name.
     *
     * @param tree the YAML of the file, read with its anchors, aliases and merge keys resolved
     * @return the theme, or {@code null} for an empty file
     * @throws IllegalArgumentException when the theme writes one of its colours another way than {@code #rrggbb}, or
     *                                  the file is not a theme
     */
    private static @Nullable TableTheme bind(@Nullable Object tree) {
        if (tree == null) {
            return null;
        }
        // The colours are a key of the file, not of the theme it describes: each part that takes a colour has it.
        var named = tree instanceof Map<?, ?> keys ? keys.remove(COLOURS) : null;
        Map<String, String> colours = MAPPER.convertValue(named, NAMED_COLOURS);
        if (colours != null) {
            colours.values().forEach(ThemeStyle::requireColour);
        }
        try {
            return MAPPER.readerFor(TableTheme.class)
                    .withAttribute(ThemeColourReader.COLOURS, colours == null ? Map.of() : colours)
                    .readValue(MAPPER.<JsonNode>valueToTree(tree));
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private static Map<String, TableTheme> sortedByName(Map<String, TableTheme> themes) {
        var byName = Comparator.comparing(TableTheme::name, String.CASE_INSENSITIVE_ORDER);
        var sorted = new LinkedHashMap<String, TableTheme>();
        themes.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(byName))
                .forEach(theme -> sorted.put(theme.getKey(), theme.getValue()));
        return sorted;
    }
}
