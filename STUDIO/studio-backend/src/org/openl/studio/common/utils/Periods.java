package org.openl.studio.common.utils;

import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * Reads the periods settings are written in, such as how long the audit log keeps its events.
 *
 * <p>A period is a number with its unit: {@code 90d} days, {@code 2w} weeks, {@code 6m} months or {@code 1y} years. It
 * can also be written in ISO 8601, such as {@code P1Y6M}. The case of the letters does not matter.
 *
 * <p>A number without its unit names no period. It is refused rather than read as days, so a {@code 6} meant as months
 * is never taken for six days.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Periods {

    /**
     * Reads a period longer than zero.
     *
     * @param text the period; empty for none
     * @return the period, or nothing for an empty text
     * @throws IllegalArgumentException when the text is no period, or the period is not longer than zero
     */
    public static @Nullable Period parse(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        var upper = text.strip().toUpperCase(Locale.ROOT);
        Period period;
        try {
            period = Period.parse(upper.startsWith("P") ? upper : "P" + upper);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("'%s' is no period: write it as 90d, 2w, 6m, 1y or P1Y6M."
                    .formatted(text), e);
        }
        if (period.isZero() || period.isNegative()) {
            throw new IllegalArgumentException("'%s' is not longer than zero.".formatted(text));
        }
        return period;
    }
}
