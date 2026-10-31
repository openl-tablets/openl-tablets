package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.openl.rules.helpers.DoubleRange;

/**
 * Rules migration: removes the {@code $} sign that OpenL Tablets 7.0.0 no longer reads in a number, as the migration
 * notes of 7.0.0 describe.
 * <p>
 * In a formula, {@code $600} reads as a name that is not found, so it becomes {@code 600}, and {@code $2,500} becomes
 * {@code 2,500}. In a cell of an {@code IntRange} or a {@code DoubleRange}, {@code >= $2,500} becomes {@code >= 2500}:
 * the comma of a number with the sign goes too, as the old range read the number without it.
 * <p>
 * Only the code the compiler rejects is changed. A formula where {@code $600} names a step or a variable compiles and
 * stays, and so does a condition of a smart rule or a simple rule, which the compiler does not report at its cell.
 * <p>
 * Migrator id: {@code rules.currency-sign}.
 *
 * @author Yury Molchan
 */
public final class RulesCurrencySignMigrator extends AbstractMigrator {

    private static final Pattern NAME = Pattern.compile("\\$\\d+[KMB]?");
    private static final Pattern AMOUNT = Pattern.compile("\\$(\\d{1,3}(?:,\\d{3})+|\\d+)");
    private static final List<CodeFix> FIXES = List.of(CodeFix.identifier(NAME, name -> name.substring(1)),
            (piece, error) -> range(piece));

    public RulesCurrencySignMigrator() {
        super("rules.currency-sign",
                "remove the $ sign from numbers",
                """
                Compiles the project and removes the $ sign that no longer belongs to a number: $600 in a
                formula, which reads as a name that is not found, becomes 600, and >= $2,500 in an IntRange or
                a DoubleRange cell becomes >= 2500. A formula where $600 names a step compiles and stays, as
                does a condition of a smart or simple rule, which the compiler does not report at its cell.
                """);
    }

    @Override
    public List<Path> migrate(Path sourceFolder, MigrationContext context) throws IOException {
        return RulesCodeIO.rewrite(this, sourceFolder, context, FIXES);
    }

    /**
     * The text of a cell that does not parse because of the numbers with the sign in it, when it reads as a range
     * without the sign.
     */
    private static @Nullable String range(String piece) {
        var fixed = AMOUNT.matcher(piece).replaceAll(amount -> amount.group(1).replace(",", ""));
        return !fixed.equals(piece) && isRange(fixed) ? fixed : null;
    }

    private static boolean isRange(String text) {
        try {
            new DoubleRange(text);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
