package org.openl.studio.projects.service.tables.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Covers laying one look of a theme over another. Every attribute of a style is laid over, including one the model
 * gains later: the looks are filled by their components.
 */
class ThemeModelTest {

    private static final String BLACK = "#000000";
    private static final String WHITE = "#ffffff";

    @Test
    void laysEveryAttributeOfAStyleOver() throws ReflectiveOperationException {
        var under = filled(ThemeStyle.class, false);
        var over = filled(ThemeStyle.class, true);

        assertEquals(over, under.with(over), "Every attribute the look on top names wins");
        assertEquals(under, under.with(ThemeStyle.NONE), "A look naming nothing keeps every attribute");
        assertEquals(over, ThemeStyle.NONE.with(over), "An attribute only the look on top names is taken");
    }

    /** A record with every component set, to the first or the second sample of its type. */
    private static <R extends Record> R filled(Class<R> type, boolean second) throws ReflectiveOperationException {
        var types = Arrays.stream(type.getRecordComponents()).map(RecordComponent::getType).toArray(Class<?>[]::new);
        var values = new Object[types.length];
        for (var i = 0; i < types.length; i++) {
            values[i] = sample(types[i], second);
        }
        return type.getDeclaredConstructor(types).newInstance(values);
    }

    /** The first or the second sample of a type: two values of it that differ. */
    private static Object sample(Class<?> type, boolean second) throws ReflectiveOperationException {
        if (type.isRecord()) {
            return filled(type.asSubclass(Record.class), second);
        }
        if (type.isEnum()) {
            var constants = type.getEnumConstants();
            return constants[second ? constants.length - 1 : 0];
        }
        if (type == Boolean.class) {
            return second;
        }
        if (type == Integer.class) {
            return second ? 12 : 10;
        }
        if (type == String.class) {
            // A colour, which a font may be named as well.
            return second ? WHITE : BLACK;
        }
        throw new IllegalArgumentException("No sample of " + type.getName());
    }
}
