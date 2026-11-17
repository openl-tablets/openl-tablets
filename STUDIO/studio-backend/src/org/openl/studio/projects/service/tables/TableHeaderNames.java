package org.openl.studio.projects.service.tables;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IOpenLTable;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * The name a table declares in its header, for the kinds of table whose header may leave it out.
 *
 * <p>A Conditions, Actions, Returns, Constants, Properties or Environment table is headed by its keyword and an
 * optional name. A Test or a Run table is headed by its keyword, the table it calls and an optional name. Any other
 * table carries its name in a signature of its own.
 *
 * <p>The header is read the way OpenL reads it: its words are parted by spaces, and a name quoted in backticks is one
 * word, spaces and all.
 *
 * @author Yury Molchan
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TableHeaderNames {

    /** What parts the words of a header, as OpenL parts them. */
    private static final String SPACES = " \n\r";

    /**
     * Whether the header of the table leaves its name out, such as a Conditions table headed by its keyword alone or a
     * Test table that names only the table it calls. A table that carries its name in a signature is never unnamed.
     */
    public static boolean isUnnamed(TableSyntaxNode node) {
        var position = namePosition(node.getNodeType());
        return position > 0 && wordsOf(node).length <= position;
    }

    /**
     * The header of a copy of the table under the given name.
     *
     * <p>The words before the name are kept as they are written, and whatever follows the name is left out. Without a
     * name the copy is left unnamed.
     *
     * @param node the table being copied
     * @param name the name of the copy, or {@code null} for none
     * @return the header, or {@code null} for a table that carries its name in a signature
     */
    static @Nullable String headerOf(TableSyntaxNode node, @Nullable String name) {
        var position = namePosition(node.getNodeType());
        if (position < 0) {
            return null;
        }
        var words = wordsOf(node);
        var kept = Arrays.stream(words, 0, Math.min(position, words.length)).map(IdentifierNode::getOriginalText);
        var named = name == null ? kept : Stream.concat(kept, Stream.of(name));
        return named.collect(Collectors.joining(" "));
    }

    /**
     * The name a copy of the table is shown under once it is written: the name it is given, or else the name OpenL
     * Studio reads from its header.
     *
     * @param source the table being copied
     * @param name   the name of the copy, or {@code null} for none
     * @return the name, empty for a table that carries its name in a signature and is given none
     */
    public static String shownName(IOpenLTable source, @Nullable String name) {
        if (name != null) {
            return name;
        }
        var node = source.getSyntaxNode();
        var header = headerOf(node, null);
        return header == null ? "" : TableSyntaxNodeUtils.str2name(header, node.getNodeType());
    }

    /** Where the name stands among the words of the header, or {@code -1} when it is part of a signature. */
    private static int namePosition(XlsNodeTypes type) {
        return switch (type) {
            case XLS_CONDITIONS, XLS_ACTIONS, XLS_RETURNS, XLS_CONSTANTS, XLS_PROPERTIES, XLS_ENVIRONMENT -> 1;
            case XLS_TEST_METHOD, XLS_RUN_METHOD -> 2;
            default -> -1;
        };
    }

    private static IdentifierNode[] wordsOf(TableSyntaxNode node) {
        try {
            return Tokenizer.tokenize(node.getHeader().getModule(), SPACES);
        } catch (OpenLCompilationException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }
}
