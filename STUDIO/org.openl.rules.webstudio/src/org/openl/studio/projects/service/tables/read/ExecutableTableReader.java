package org.openl.studio.projects.service.tables.read;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.openl.rules.lang.xls.syntax.HeaderSyntaxNode;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.ArgumentView;
import org.openl.studio.projects.model.tables.ExecutableView;

/**
 * Abstract class for reading executable tables.
 *
 * @author Vladyslav Pikus
 */
public abstract class ExecutableTableReader<T extends ExecutableView, R extends ExecutableView.Builder<?>> extends EditableTableReader<T, R> {

    public ExecutableTableReader(Supplier<R> builderCreator) {
        super(builderCreator);
    }

    @Override
    protected void initialize(R builder, IOpenLTable openLTable) {
        super.initialize(builder, openLTable);
        initializeSignature(builder, openLTable.getSyntaxNode().getHeader());
    }

    private void initializeSignature(R builder, HeaderSyntaxNode header) {
        var headerSource = header.getSourceString();

        var parsedTableType = readIdentifier(headerSource, 0);
        builder.tableType(parsedTableType.identifier());

        int pos = rollWhitespaces(headerSource, parsedTableType.pos());
        if (header.isCollect()) {
            // skip "Collect" keyword
            pos = rollIdentifier(headerSource, pos);
        }

        var parsedReturnType = readIdentifier(headerSource, pos);
        builder.returnType(parsedReturnType.identifier());

        var parsedName = readIdentifier(headerSource, parsedReturnType.pos());
        builder.name(parsedName.identifier());

        pos = rollWhitespaces(headerSource, parsedName.pos());
        if (pos < headerSource.length() && headerSource.charAt(pos) == '(') {
            var args = parseArguments(headerSource, pos);
            builder.args(args);
        }
    }

    private static List<ArgumentView> parseArguments(String headerSource, int pos) {
        List<ArgumentView> args = new ArrayList<>();
        pos++;
        while (pos < headerSource.length() && headerSource.charAt(pos) != ')') {
            var parsedType = readIdentifier(headerSource, pos);
            var parsedName = readIdentifier(headerSource, parsedType.pos());

            if (parsedType.hasIdentifier() || parsedName.hasIdentifier()) {
                args.add(ArgumentView.builder()
                        .type(parsedType.identifier())
                        .name(parsedName.identifier())
                        .build());
            }

            pos = rollWhitespaces(headerSource, parsedName.pos());
            if (pos < headerSource.length() && headerSource.charAt(pos) == ',') {
                pos++;
            } else {
                // no char ',' - end of arguments
                break;
            }
        }
        return Collections.unmodifiableList(args);
    }

    private static ParsedIdentifier readIdentifier(String source, int from) {
        int pos = rollWhitespaces(source, from);
        int start = pos;
        pos = rollIdentifier(source, pos);
        return start < pos
                ? new ParsedIdentifier(pos, source.substring(start, pos))
                : new ParsedIdentifier(pos, null);
    }

    static int rollIdentifier(String s, int pos) {
        while (pos < s.length() && isIdentifierPart(s.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    private static boolean isIdentifierPart(char cp) {
        return Character.isJavaIdentifierPart(cp) || "[].<>:".indexOf(cp) > -1;
    }

    static int rollWhitespaces(String s, int pos) {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    private record ParsedIdentifier(int pos, String identifier) {

        public boolean hasIdentifier() {
            return identifier != null;
        }

    }
}
