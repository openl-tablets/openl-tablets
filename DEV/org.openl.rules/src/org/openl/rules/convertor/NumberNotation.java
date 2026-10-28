package org.openl.rules.convertor;

/**
 * The ways business users write numbers, shared by formulas, cells and ranges.
 *
 * @author Yury Molchan
 */
public final class NumberNotation {

    private NumberNotation() {
    }

    /**
     * Returns the value of a multiplier suffix: 1000 for {@code K}, 1000000 for {@code M} and 1000000000 for
     * {@code B}. Any other character, also a small letter, is not a multiplier, and its value is 1.
     */
    public static long multiplier(char suffix) {
        return switch (suffix) {
            case 'K' -> 1_000L;
            case 'M' -> 1_000_000L;
            case 'B' -> 1_000_000_000L;
            default -> 1L;
        };
    }
}
