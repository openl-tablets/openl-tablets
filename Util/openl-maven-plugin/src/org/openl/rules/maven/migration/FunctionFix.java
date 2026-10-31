package org.openl.rules.maven.migration;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.binding.exception.MethodNotFoundException;
import org.openl.message.OpenLErrorMessage;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;

/**
 * Replaces a call of a function that is not found for the argument types with the code of a template.
 * <p>
 * The fix applies only to a call of the function by its name, not to a method called on a value. The types of the
 * arguments, as the compiler reports them, must be the types of the fix: a primitive type counts as its wrapper, and
 * a subtype counts as its supertype.
 * <p>
 * An argument keeps its text. It is put in parentheses only where the template places it next to an operator and
 * the argument is not a single operand. A call stays for a manual fix when the template writes an argument twice and
 * the argument calls something, as the rewritten rule would run that call twice.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor
final class FunctionFix implements CodeFix {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)}");

    private final String function;
    private final String template;
    private final Class<?>[] types;

    @Override
    public @Nullable String fix(String piece, OpenLErrorMessage error) {
        if (!(error.getError().getCause() instanceof MethodNotFoundException notFound)
                || !function.equals(notFound.getMethodName()) || !accepts(notFound.getParams())) {
            return null;
        }
        var arguments = CodeText.arguments(piece, function);
        return arguments == null || arguments.size() != types.length || repeatsCall(arguments)
                ? null
                : render(arguments);
    }

    private boolean repeatsCall(List<String> arguments) {
        return PLACEHOLDER.matcher(template)
                .results()
                .collect(Collectors.groupingBy(placeholder -> Integer.parseInt(placeholder.group(1)),
                        Collectors.counting()))
                .entrySet()
                .stream()
                .anyMatch(uses -> uses.getValue() > 1 && CodeText.hasCall(arguments.get(uses.getKey())));
    }

    private boolean accepts(IOpenClass[] params) {
        return params.length == types.length && IntStream.range(0, types.length).allMatch(i -> {
            var type = params[i].getInstanceClass();
            return type != null && ClassUtils.isAssignable(type, types[i]);
        });
    }

    private String render(List<String> arguments) {
        var matcher = PLACEHOLDER.matcher(template);
        var code = new StringBuilder();
        while (matcher.find()) {
            var argument = arguments.get(Integer.parseInt(matcher.group(1)));
            var operand = CodeText.operand(argument, template, matcher.start(), matcher.end());
            matcher.appendReplacement(code, Matcher.quoteReplacement(operand));
        }
        return matcher.appendTail(code).toString();
    }
}
