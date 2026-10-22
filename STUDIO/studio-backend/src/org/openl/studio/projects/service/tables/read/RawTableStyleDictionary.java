package org.openl.studio.projects.service.tables.read;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.openl.studio.projects.model.tables.RawTableCellStyle;

/**
 * The styles the cells of one read use, each once, numbered in the order the read meets them.
 *
 * <p>A cell and a piece of its text name their style by its number, so a style a thousand cells share is reported
 * once.
 *
 * @author Yury Molchan
 */
public final class RawTableStyleDictionary {

    private final Map<RawTableCellStyle, Integer> numbers = new LinkedHashMap<>();

    /**
     * The number of a style, which a style met for the first time takes.
     *
     * @param style the style, or {@code null} for none
     * @return the number of the style, or {@code null} for none
     */
    public @Nullable Integer numberOf(@Nullable RawTableCellStyle style) {
        return style == null ? null : numbers.computeIfAbsent(style, met -> numbers.size());
    }

    /** The styles met, each at its number. */
    public List<RawTableCellStyle> styles() {
        return List.copyOf(numbers.keySet());
    }
}
