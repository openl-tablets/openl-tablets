package org.openl.excel.parser.markdown;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * The value a text of a cell of a Markdown file reads as, of the type a workbook gives the same value.
 *
 * <p>A workbook keeps the type of each cell: a number, a boolean, a date or a text. A file keeps text alone, so a text
 * reads as the value a workbook holds for it: a whole number as an {@code Integer}, {@code true} and {@code false} as a
 * {@code Boolean}, and a date as a {@code Date} of the time zone of the reader, the day alone as {@code 2026-03-02} or
 * with its time as {@code 2026-03-02T10:30:15}.
 *
 * <p>A text reads as a value only when the value is written back as the same text, see
 * {@link MarkdownGrid#textOf(Object)}, so no text is lost: {@code 007}, {@code +3}, {@code 3.0} or {@code 03/02/2026}
 * stay texts, and the type a table declares reads them as before. Any other number stays a text too: it reads as a
 * {@code Double} wherever no type is declared, and a {@code Double} would round the digits of a long one. A formula,
 * which starts with {@code =}, stays a text.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class MarkdownValues {

    /** The most digits a whole number an {@code int} holds can have. */
    private static final int INT_DIGITS = 10;

    /**
     * Returns the value a text of a cell reads as.
     *
     * @param text the text of a cell, or {@code null} for an empty one
     * @return the whole number, the boolean or the date the text is written as, or the text itself
     */
    static @Nullable Object of(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        var value = switch (text) {
            case "true", "false" -> Boolean.valueOf(text);
            default -> looksLikeDate(text) ? dateOf(text) : wholeNumberOf(text);
        };
        return value != null && text.equals(MarkdownGrid.textOf(value)) ? value : text;
    }

    /**
     * Whether a text has the shape of a date, {@code 2026-03-02} or {@code 2026-03-02T10:30:15}: a parser that throws
     * on a text of another kind costs far more than the check.
     */
    private static boolean looksLikeDate(String text) {
        var length = text.length();
        return (length == 10 || length >= 19 && text.charAt(10) == 'T') && text.charAt(4) == '-'
                && text.charAt(7) == '-';
    }

    private static @Nullable Date dateOf(String text) {
        try {
            var time = text.length() == 10 ? LocalDate.parse(text).atStartOfDay() : LocalDateTime.parse(text);
            return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException e) {
            // Not a day of the calendar, such as 2026-13-45, or not a date at all: a text
            return null;
        }
    }

    /**
     * The whole number of up to ten digits, with a minus before them or not, that an {@code int} holds, as a workbook
     * reads a whole number. The digits are checked by hand, so that a text of another kind, such as the range
     * {@code 18-65}, costs no parser that throws.
     */
    private static @Nullable Integer wholeNumberOf(String text) {
        var start = text.charAt(0) == '-' ? 1 : 0;
        var digits = text.length() - start;
        if (digits < 1 || digits > INT_DIGITS) {
            return null;
        }
        for (var i = start; i < text.length(); i++) {
            var c = text.charAt(i);
            if (c < '0' || c > '9') {
                return null;
            }
        }
        var number = Long.parseLong(text);
        return number == (int) number ? (int) number : null;
    }
}
