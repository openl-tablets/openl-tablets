package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Groovy migration: removes the type arguments from a static member reached through a parameterized type, such as
 * {@code Literal<Node>.of(true)}, which Groovy 6 rejects with {@code Cannot refer to a static member of a generic type
 * through a parameterization}. The call becomes {@code Literal.of(true)}, as the migration notes of OpenL Tablets 7.0.0
 * describe, and keeps its behavior.
 * <p>
 * A type is a name that starts with a capital letter, and its type arguments hold only names, wildcards, arrays and
 * nested type arguments. A member is reached when a dot and a name follow the type arguments. Texts and comments stay
 * as they are.
 * <p>
 * Migrator id: {@code groovy.generic-static}.
 *
 * @author Yury Molchan
 */
public final class GroovyGenericStaticMigrator extends AbstractMigrator {

    private static final Pattern TYPE = Pattern.compile("\\b[A-Z][\\w$]*<");
    private static final String TYPE_ARGUMENT = " \t.,?&[]<>";

    public GroovyGenericStaticMigrator() {
        super("groovy.generic-static",
                "groovy static members without type arguments",
                """
                Removes the type arguments from a static member reached through a parameterized type in every
                .groovy file under the project: Literal<Node>.of(true) becomes Literal.of(true), which Groovy 6
                requires and which keeps the behavior. A type is a name that starts with a capital letter.
                """);
    }

    @Override
    public List<Path> migrate(Path sourceFolder, MigrationContext context) throws IOException {
        return GroovySourcesIO.rewrite(sourceFolder, GroovyGenericStaticMigrator::migrate);
    }

    /**
     * Removes the type arguments of the types whose static members the source reaches.
     */
    static String migrate(String source) {
        var result = new StringBuilder(source.length());
        var copied = 0;
        var from = 0;
        var literals = GroovyText.literals(source);
        var type = TYPE.matcher(source);
        // the search goes on after the removed type arguments, as a type in them is removed with them
        while (type.find(from)) {
            var open = type.end() - 1;
            var close = closing(source, open);
            from = type.end();
            if (!literals.get(open) && close > 0 && isMember(source, close + 1)) {
                result.append(source, copied, open);
                copied = close + 1;
                from = copied;
            }
        }
        return result.append(source, copied, source.length()).toString();
    }

    /**
     * Returns the position of the {@code >} that closes the type arguments opened at the position, or {@code -1}
     * when the text there is not type arguments.
     */
    private static int closing(String source, int open) {
        var depth = 0;
        for (var i = open; i < source.length(); i++) {
            var c = source.charAt(i);
            if (!Character.isJavaIdentifierPart(c) && TYPE_ARGUMENT.indexOf(c) < 0) {
                return -1;
            }
            depth += switch (c) {
                case '<' -> 1;
                case '>' -> -1;
                default -> 0;
            };
            if (depth == 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Whether a dot and a name start at the position.
     */
    private static boolean isMember(String source, int position) {
        return position + 1 < source.length() && source.charAt(position) == '.'
                && Character.isJavaIdentifierStart(source.charAt(position + 1));
    }
}
