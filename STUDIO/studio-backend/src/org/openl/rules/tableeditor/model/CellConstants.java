package org.openl.rules.tableeditor.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import org.openl.rules.constants.ConstantOpenField;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;

/**
 * The constants of a module a cell may hold instead of a value it writes out, such as {@code Const16} in a number
 * cell.
 *
 * @author Yury Molchan
 */
public final class CellConstants {

    /** No constant: a module that is not compiled, or a cell read on its own. */
    public static final CellConstants NONE = new CellConstants(List.of());

    private final List<ConstantOpenField> constants;
    /** The names already found for a type: every number cell of a table asks for its type. */
    private final Map<Class<?>, List<String>> namesByType = new ConcurrentHashMap<>();

    private CellConstants(List<ConstantOpenField> constants) {
        this.constants = constants;
    }

    /**
     * The constants of a compiled module, those of the modules it depends on included.
     *
     * @param module the module, or {@code null} when it is not compiled
     */
    public static CellConstants of(@Nullable IOpenClass module) {
        if (module == null) {
            return NONE;
        }
        var constants = module.getFields()
                .stream()
                .filter(ConstantOpenField.class::isInstance)
                .map(ConstantOpenField.class::cast)
                .toList();
        return constants.isEmpty() ? NONE : new CellConstants(constants);
    }

    /**
     * The names of the constants whose values a cell of the given type takes, sorted.
     */
    List<String> namesFor(Class<?> type) {
        return constants.isEmpty() ? List.of() : namesByType.computeIfAbsent(type, this::namesOf);
    }

    private List<String> namesOf(Class<?> type) {
        return constants.stream()
                .filter(constant -> ClassUtils.isAssignable(constant.getType().getInstanceClass(), type))
                .map(ConstantOpenField::getName)
                .sorted()
                .distinct()
                .toList();
    }
}
