package org.openl.rules.rest.service.tables.read;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.openl.rules.rest.model.tables.ArgumentView;
import org.openl.rules.rest.model.tables.ExecutableView;
import org.openl.rules.table.IOpenLTable;
import org.openl.util.StringUtils;

/**
 * TODO description
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
        initializeSignature(builder, openLTable.getSyntaxNode().getHeader().getSourceString());
    }

    private void initializeSignature(R builder, String headerSource) {
        int pos = rollWhitespaces(headerSource, 0);
        int start = pos;
        pos = rollIdentifier(headerSource, pos);
        if (start < pos) {
            // it is probably table kind
            builder.kind(headerSource.substring(start, pos));
        }
        pos = rollWhitespaces(headerSource, pos);
        start = pos;
        pos = rollIdentifier(headerSource, pos);
        if (start < pos) {
            // it is probably table return type
            builder.returnType(headerSource.substring(start, pos));
        }
        pos = rollWhitespaces(headerSource, pos);
        start = pos;
        pos = rollIdentifier(headerSource, pos);
        if (start < pos) {
            // it is probably table name
            builder.name(headerSource.substring(start, pos));
        }
        pos = rollWhitespaces(headerSource, pos);
        List<ArgumentView> args = null;
        if (pos < headerSource.length() && headerSource.charAt(pos) == '(') {
            args = parseArguments(headerSource, pos);
        }
        builder.args(args);
    }

    protected List<ArgumentView> getArgs(String headerSource, int start) {
        var pos = StringUtils.first(headerSource, start, headerSource.length(), x -> x == '(');
        if (pos < 0) {
            return List.of();
        }
        return parseArguments(headerSource, pos);
    }

    private List<ArgumentView> parseArguments(String headerSource, int pos) {
        List<ArgumentView> args = new ArrayList<>();
        pos++;
        while (pos < headerSource.length() && headerSource.charAt(pos) != ')') {
            var argBuilder = ArgumentView.builder();
            pos = rollWhitespaces(headerSource, pos);
            int start = pos;
            pos = rollIdentifier(headerSource, pos);
            if (start < pos) {
                // it is probably argument type
                argBuilder.type(headerSource.substring(start, pos));
            }
            pos = rollWhitespaces(headerSource, pos);
            start = pos;
            pos = rollIdentifier(headerSource, pos);
            if (start < pos) {
                // it is probably argument name
                argBuilder.name(headerSource.substring(start, pos));
            }
            args.add(argBuilder.build());
            pos = rollWhitespaces(headerSource, pos);
            if (pos < headerSource.length() && headerSource.charAt(pos) == ',') {
                pos++;
            } else {
                // no char ',' - end of arguments
                break;
            }
        }
        return Collections.unmodifiableList(args);
    }

    private static int rollIdentifier(String s, int pos) {
        while (pos < s.length() && isIdentifierPart(s.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    private static boolean isIdentifierPart(char cp) {
        return Character.isJavaIdentifierPart(cp) || "[].<>:".indexOf(cp) > -1;
    }

    private static int rollWhitespaces(String s, int pos) {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
            pos++;
        }
        return pos;
    }
}
