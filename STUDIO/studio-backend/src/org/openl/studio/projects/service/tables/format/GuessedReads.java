package org.openl.studio.projects.service.tables.format;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

import org.openl.rules.datatype.binding.DatatypeHelper;
import org.openl.rules.dt.DecisionTableColumnHeaders;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.studio.projects.model.tables.RawTableSignaturePart;
import org.openl.util.StringUtils;

/**
 * How a table the compiler read none of is read from its text: whether it is written with its rows and columns
 * swapped, and what its header names.
 *
 * <p>A table the compiler could not read, because a type it names is missing or the table it tests does not exist,
 * still has the parts its kind writes. They are found where the kind writes them and by what their cells hold: field
 * names, the types of fields, the kinds of the columns of a Rules table. A table whose text tells nothing is read as
 * it is written.
 *
 * @author Yury Molchan
 */
final class GuessedReads {

    /**
     * A field name as a Data, a Test or a Run table writes it: a chain of names, each with an optional index, such as
     * {@code order.lines[0].price} or {@code _res_.$Value$Total}, and the precision a Test may add, such as
     * {@code _res_ (2)}. A field name starts with a lower-case letter, {@code _} or {@code $}, which tells it from a
     * title or a value such as {@code Code} or {@code A1}.
     */
    private static final Pattern FIELD = Pattern.compile(
            "[\\p{Ll}_$][\\w$]*+(\\[[^]]*+])?+(\\.[\\p{L}_$][\\w$]*+(\\[[^]]*+])?+)*+(\\s*+\\(\\s*+-?\\d++\\s*+\\))?+",
            Pattern.UNICODE_CHARACTER_CLASS);

    /** A type as a Datatype or a Constants table writes it: a name, a package, the brackets of an array. */
    private static final Pattern TYPE = Pattern.compile("[\\p{L}_$][\\w$.]*+(<[^>]*+>)?+(\\s*+\\[\\s*+])*+",
            Pattern.UNICODE_CHARACTER_CLASS);

    /** The primitive types, the only types whose names start with a lower-case letter. */
    private static final Set<String> PRIMITIVES = Set.of("boolean", "byte", "char", "short", "int", "long", "float",
            "double");

    /** The kind of a column of a Rules table, such as {@code C1}, {@code HC2}, {@code RET1} or {@code RULE}. */
    private static final Pattern COLUMN_KIND = Pattern.compile("(?i)(" + String.join("|",
            Arrays.stream(DecisionTableColumnHeaders.values()).map(DecisionTableColumnHeaders::getHeaderKey).toList())
            + ")\\d*");

    /** The kinds of the columns of a Rules table, by the key a text names them with. */
    private static final Map<String, DecisionTableColumnHeaders> COLUMN_KINDS = Arrays
            .stream(DecisionTableColumnHeaders.values())
            .collect(Collectors.toUnmodifiableMap(DecisionTableColumnHeaders::getHeaderKey, Function.identity()));

    private GuessedReads() {
    }

    /**
     * Whether a Data, a Test or a Run table is written transposed: its field names stand down the first column and
     * not across the first row. A table whose field names stand in both, or in neither, is read as it is written.
     *
     * @param body the body of the table
     * @return {@code true} for a table written with a field in each row
     */
    static boolean fieldsTurned(ILogicalTable body) {
        return !allOf(body, GuessedReads::isField) && allOf(body.transpose(), GuessedReads::isField);
    }

    /**
     * Whether a Datatype or a Constants table is written transposed: a field in each column. Titles naming the type
     * and the name of a field tell where they stand, as the compiler reads them. With no titles, a body of one column,
     * or one wider than the titles a Datatype can name, keeps a field in each column, and so does one whose first row,
     * not its first column, holds only types.
     *
     * @param body the body of the table
     * @return {@code true} for a table written with a field in each column
     */
    static boolean typesTurned(ILogicalTable body) {
        if (DatatypeHelper.hasColumnTitles(body) || DatatypeHelper.hasColumnTitles(body.transpose())) {
            return !DatatypeHelper.hasColumnTitles(body);
        }
        if (body.getHeight() == 1) {
            return false;
        }
        if (body.getWidth() == 1 || body.getWidth() > DatatypeHelper.COLUMN_TITLES.size()) {
            return true;
        }
        return allOf(body, GuessedReads::isType) && !allOf(body.transpose(), GuessedReads::isType);
    }

    /**
     * Whether a Rules table is written with a rule in each column: the kinds of its columns, such as {@code C1} or
     * {@code RET1}, stand down its first column and not across its first row.
     *
     * @param body the body of the table
     * @return {@code true} for a table written with a rule in each column
     */
    static boolean kindsTurned(ILogicalTable body) {
        Predicate<String> isKind = text -> columnKindOf(text) != null;
        return !anyOf(body, isKind) && anyOf(body.transpose(), isKind);
    }

    /**
     * The kind of a column of a Rules table its text names, such as {@code C1} or {@code RET1}, in any case.
     *
     * @param text the text of a cell
     * @return the kind, or {@code null} for a text naming none
     */
    static @Nullable DecisionTableColumnHeaders columnKindOf(@Nullable String text) {
        var kind = text == null ? null : COLUMN_KIND.matcher(text.trim());
        return kind != null && kind.matches() ? COLUMN_KINDS.get(kind.group(1).toUpperCase(Locale.ROOT)) : null;
    }

    /**
     * Whether a Datatype header names the type of the values of a Vocabulary, such as
     * {@code Datatype Gender <String>}: the compiler reads three words, the last in angle brackets.
     *
     * @param header the text of the header
     * @return {@code true} for the header of a Vocabulary
     */
    static boolean isVocabulary(String header) {
        var words = header.trim().split("\\s+");
        return words.length == 3 && words[2].startsWith("<") && words[2].endsWith(">");
    }

    /**
     * The type a header names besides the table, such as the type a rule returns.
     *
     * @param header the text of the header
     * @return the type, trimmed, or an empty text for a header naming none
     */
    static String typeOf(String header) {
        var from = 0;
        var type = new StringBuilder();
        for (var piece : SignatureSplitter.split(header)) {
            if (piece.part() == RawTableSignaturePart.TYPE) {
                type.append(header, from, piece.to());
            }
            from = piece.to();
        }
        return type.toString().trim();
    }

    /**
     * How many parameters a header names.
     *
     * @param header the text of the header
     * @return the count of the parameters
     */
    static int parametersOf(String header) {
        return (int) SignatureSplitter.split(header)
                .stream()
                .filter(piece -> piece.part() == RawTableSignaturePart.PARAMETER_NAME)
                .count();
    }

    /** The text of the header of a table, or an empty text for a header that holds none. */
    static String headerOf(TableSyntaxNode node) {
        return Objects.requireNonNullElse(node.getGridTable().getCell(0, 0).getStringValue(), "");
    }

    /** The text of a cell of a body, trimmed, or an empty text for a blank cell. */
    static String textAt(ILogicalTable body, int column, int row) {
        return StringUtils.trimToEmpty(body.getCell(column, row).getStringValue());
    }

    /** Whether a text is a field name, letters of any script included. */
    static boolean isField(String text) {
        return FIELD.matcher(text).matches();
    }

    /** Whether a text names a type: a name starting with a capital, or a primitive type, an array of it included. */
    static boolean isType(String text) {
        if (!TYPE.matcher(text).matches()) {
            return false;
        }
        var bracket = text.indexOf('[');
        var name = (bracket < 0 ? text : text.substring(0, bracket)).trim();
        return Character.isUpperCase(text.charAt(0)) || PRIMITIVES.contains(name);
    }

    /** Whether every cell of the first row of a body that holds text holds a text the test accepts. */
    private static boolean allOf(ILogicalTable body, Predicate<String> accepted) {
        var texts = firstRow(body);
        return !texts.isEmpty() && texts.stream().allMatch(accepted);
    }

    /** Whether some cell of the first row of a body holds a text the test accepts. */
    private static boolean anyOf(ILogicalTable body, Predicate<String> accepted) {
        return firstRow(body).stream().anyMatch(accepted);
    }

    /** The texts of the cells of the first row of a body that hold text. */
    private static List<String> firstRow(ILogicalTable body) {
        if (body.getHeight() == 0) {
            return List.of();
        }
        return IntStream.range(0, body.getWidth())
                .mapToObj(column -> textAt(body, column, 0))
                .filter(text -> !text.isEmpty())
                .toList();
    }
}
