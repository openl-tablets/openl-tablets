package org.openl.rules.maven.migration;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.openl.message.OpenLErrorMessage;

/**
 * Rewrites a piece of rules code that no longer compiles, such as a call of a removed function.
 * <p>
 * A fix gets the piece the compiler reports and the error about it. It returns the code to put in its place, or
 * {@code null} when the error is not the one the fix is for. The returned code needs no parentheses: they are added
 * where the place requires them.
 *
 * @author Yury Molchan
 */
@FunctionalInterface
interface CodeFix {

    @Nullable
    String fix(String piece, OpenLErrorMessage error);

    /**
     * Replaces a call of a function that is not found for the argument types.
     *
     * @param function the name of the function
     * @param template the code to put in place of the call, where {@code {0}}, {@code {1}} and so on stand for the
     *                 arguments of the call
     * @param types    the types the arguments must be, such as {@code Number.class} for any number, a primitive one
     *                 included
     */
    static CodeFix function(String function, String template, Class<?>... types) {
        return new FunctionFix(function, template, types);
    }

    /**
     * Replaces a name that is not found, such as a removed constant.
     */
    static CodeFix identifier(String name, String replacement) {
        return identifier(Pattern.compile(Pattern.quote(name)), x -> replacement);
    }

    /**
     * Replaces a name that is not found and matches the pattern as a whole with the code made from it.
     */
    static CodeFix identifier(Pattern name, UnaryOperator<String> replacement) {
        return (piece, error) -> name.matcher(piece).matches() ? replacement.apply(piece) : null;
    }
}
