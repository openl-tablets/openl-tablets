package org.openl.binding.impl;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.binding.exception.AmbiguousMethodException;

/**
 * The order of expanding:
 * <ul>
 * <li>byte -> short -> int -> long -> float -> double</li>
 * <li>char -> int</li>
 * <li>Byte -> Short -> Integer -> Long -> Float -> Double -> BigDecimal</li>
 * <li>Character -> Integer</li>
 * <li>Long -> BigInteger -> BigDecimal</li>
 * </ul>
 * <p>
 * The order of boxing:
 * <ul>
 * <li>primitives -> boxed -> valued</li>
 * <li>boxed -> superClasses</li>
 * <li>valued -> superClasses</li>
 * <li>boxed -> primitives</li>
 * </ul>
 */
class MethodSearchOverloadTest extends AbstractMethodSearchTest {
    private static final Class<?> target = OverloadedMethods.class;
    private static final Class<?>[] primitives = new Class[]{byte.class,
            short.class,
            int.class,
            long.class,
            float.class,
            double.class};
    private static final Class<?>[] boxed = new Class[]{Byte.class,
            Short.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            BigInteger.class,
            BigDecimal.class};
    private static final Class<?>[] nonNumbers = new Class[]{boolean.class,
            char.class,
            Boolean.class,
            Character.class};
    private static final Class<?>[] primitivesArray = new Class[]{byte[].class,
            short[].class,
            int[].class,
            long[].class,
            float[].class,
            double[].class};
    private static final Class<?>[] boxedArray = new Class[]{Byte[].class,
            Short[].class,
            Integer[].class,
            Long[].class,
            Float[].class,
            Double[].class,
            BigInteger[].class,
            BigDecimal[].class};
    private static final Class<?>[] nonNumbersArray = new Class[]{boolean[].class,
            char[].class,
            Boolean[].class,
            Character[].class};

    @Test
    void testSearch() throws AmbiguousMethodException {
        assertMethod(target, "m0_prim", primitives, "int", "int", "int", "float", "float", NF);
        assertMethod(target, "m0_prim", boxed, "int", "int", "int", "float", "float", NF, NF, NF);
        assertMethod(target, "m0_prim", nonNumbers, NF, "int", NF, "int");

        assertMethod(target, "m0_Boxed", primitives, "Integer", "Integer", "Integer", "Float", "Float", NF);
        assertMethod(target, "m0_Boxed", boxed, "Integer", "Integer", "Integer", "Float", "Float", NF, NF, NF);
        assertMethod(target, "m0_Boxed", nonNumbers, NF, "Integer", NF, "Integer");

        assertMethod(target, "m0_mixed", primitives, "int", "int", "int", "float", "float", "BigDecimal");
        assertMethod(target,
                "m0_mixed",
                boxed,
                "Short",
                "Short",
                "BigDecimal",
                "BigDecimal",
                "BigDecimal",
                "BigDecimal",
                "BigDecimal",
                "BigDecimal");
        assertMethod(target, "m0_mixed", nonNumbers, NF, "int", NF, "BigDecimal");

        assertMethod(target, "m0_comp", primitives, "short", "short", "Comparable", "Long", "Comparable", "Comparable");
        assertMethod(target,
                "m0_comp",
                boxed,
                "Comparable",
                "Comparable",
                "Comparable",
                "Long",
                "Comparable",
                "Comparable",
                "Comparable",
                "Comparable");
        assertMethod(target, "m0_comp", nonNumbers, "Comparable", "Comparable", "Comparable", "Comparable");
    }

    @Test
    void testExpandPrimitives() throws AmbiguousMethodException {
        assertMethod(target, "_byte", primitives, "byte", NF, NF, NF, NF, NF);
        assertMethod(target, "_byte", boxed, "byte", NF, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_byte", nonNumbers, NF, NF, NF, NF);

        assertMethod(target, "_short", primitives, "short", "short", NF, NF, NF, NF);
        assertMethod(target, "_short", boxed, "short", "short", NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_short", nonNumbers, NF, NF, NF, NF);

        assertMethod(target, "_int", primitives, "int", "int", "int", NF, NF, NF);
        assertMethod(target, "_int", boxed, "int", "int", "int", NF, NF, NF, NF, NF);
        assertMethod(target, "_int", nonNumbers, NF, "int", NF, "int");

        assertMethod(target, "_long", primitives, "long", "long", "long", "long", NF, NF);
        assertMethod(target, "_long", boxed, "long", "long", "long", "long", NF, NF, NF, NF);
        assertMethod(target, "_long", nonNumbers, NF, "long", NF, "long");

        assertMethod(target, "_float", primitives, "float", "float", "float", "float", "float", NF);
        assertMethod(target, "_float", boxed, "float", "float", "float", "float", "float", NF, NF, NF);
        assertMethod(target, "_float", nonNumbers, NF, "float", NF, "float");

        assertMethod(target, "_double", primitives, "double", "double", "double", "double", "double", "double");
        assertMethod(target, "_double", boxed, "double", "double", "double", "double", "double", "double", NF, NF);
        assertMethod(target, "_double", nonNumbers, NF, "double", NF, "double");

        assertMethod(target, "_boolean", primitives, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_boolean", boxed, NF, NF, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_boolean", nonNumbers, "boolean", NF, "boolean", NF);

        assertMethod(target, "_char", primitives, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_char", boxed, NF, NF, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_char", nonNumbers, NF, "char", NF, "char");
    }

    static Stream<Arguments> testExpandBoxed() {
        return Stream.of(
                arguments("_Byte", primitives, new Object[]{"Byte", NF, NF, NF, NF, NF}),
                arguments("_Byte", boxed, new Object[]{"Byte", NF, NF, NF, NF, NF, NF, NF}),
                arguments("_Byte", nonNumbers, new Object[]{NF, NF, NF, NF}),

                arguments("_Short", primitives, new Object[]{"Short", "Short", NF, NF, NF, NF}),
                arguments("_Short", boxed, new Object[]{"Short", "Short", NF, NF, NF, NF, NF, NF}),
                arguments("_Short", nonNumbers, new Object[]{NF, NF, NF, NF}),

                arguments("_Integer", primitives, new Object[]{"Integer", "Integer", "Integer", NF, NF, NF}),
                arguments("_Integer", boxed, new Object[]{"Integer", "Integer", "Integer", NF, NF, NF, NF, NF}),
                arguments("_Integer", nonNumbers, new Object[]{NF, "Integer", NF, "Integer"}),

                arguments("_Long", primitives, new Object[]{"Long", "Long", "Long", "Long", NF, NF}),
                arguments("_Long", boxed, new Object[]{"Long", "Long", "Long", "Long", NF, NF, NF, NF}),
                arguments("_Long", nonNumbers, new Object[]{NF, "Long", NF, "Long"}),

                arguments("_Float", primitives, new Object[]{"Float", "Float", "Float", "Float", "Float", NF}),
                arguments("_Float", boxed, new Object[]{"Float", "Float", "Float", "Float", "Float", NF, NF, NF}),
                arguments("_Float", nonNumbers, new Object[]{NF, "Float", NF, "Float"}),

                arguments("_Double", primitives,
                        new Object[]{"Double", "Double", "Double", "Double", "Double", "Double"}),
                arguments("_Double", boxed,
                        new Object[]{"Double", "Double", "Double", "Double", "Double", "Double", NF, NF}),
                arguments("_Double", nonNumbers, new Object[]{NF, "Double", NF, "Double"}),

                arguments("_Boolean", primitives, new Object[]{NF, NF, NF, NF, NF, NF}),
                arguments("_Boolean", boxed, new Object[]{NF, NF, NF, NF, NF, NF, NF, NF}),
                arguments("_Boolean", nonNumbers, new Object[]{"Boolean", NF, "Boolean", NF}),

                arguments("_Character", primitives, new Object[]{NF, NF, NF, NF, NF, NF}),
                arguments("_Character", boxed, new Object[]{NF, NF, NF, NF, NF, NF, NF, NF}),
                arguments("_Character", nonNumbers, new Object[]{NF, "Character", NF, "Character"}),

                arguments("_BigInteger", primitives,
                        new Object[]{"BigInteger", "BigInteger", "BigInteger", "BigInteger", NF, NF}),
                arguments("_BigInteger", boxed,
                        new Object[]{"BigInteger", "BigInteger", "BigInteger", "BigInteger", NF, NF, "BigInteger", NF}),
                arguments("_BigInteger", nonNumbers, new Object[]{NF, "BigInteger", NF, "BigInteger"}),

                arguments("_BigDecimal", primitives, new Object[]{"BigDecimal", "BigDecimal", "BigDecimal",
                        "BigDecimal", "BigDecimal", "BigDecimal"}),
                arguments("_BigDecimal", boxed, new Object[]{"BigDecimal", "BigDecimal", "BigDecimal", "BigDecimal",
                        "BigDecimal", "BigDecimal", "BigDecimal", "BigDecimal"}),
                arguments("_BigDecimal", nonNumbers, new Object[]{NF, "BigDecimal", NF, "BigDecimal"}));
    }

    @ParameterizedTest
    @MethodSource
    void testExpandBoxed(String methodName, Class<?>[] argTypes, Object[] expected)
            throws AmbiguousMethodException {
        assertMethod(target, methodName, argTypes, expected);
    }

    @Test
    void testOneArgument() throws AmbiguousMethodException {
        assertMethod(target, "m1", primitives, "byte", "short", "int", "long", "float", "double");
        assertMethod(target,
                "m1",
                boxed,
                "Byte",
                "Short",
                "Integer",
                "Long",
                "Float",
                "Double",
                "BigInteger",
                "BigDecimal");
        assertMethod(target, "m1", nonNumbers, "boolean", "char", "Boolean", "Character");
    }

    @Test
    void testOneArgument2() throws AmbiguousMethodException {
        assertMethod(target, "m1", primitives, "byte", "short", "int", "long", "float", "double");
        assertMethod(target,
                "m1",
                boxed,
                "Byte",
                "Short",
                "Integer",
                "Long",
                "Float",
                "Double",
                "BigInteger",
                "BigDecimal");
        assertMethod(target, "m1", nonNumbers, "boolean", "char", "Boolean", "Character");
    }

    @Test
    void testTwoArguments() throws AmbiguousMethodException {
        assertMethod(target, "m2", byte.class, primitives, "long", "long", "long", "long", "double", "double");
        assertMethod(target,
                "m2",
                byte.class,
                boxed,
                "GenericByte",
                "GenericShort",
                "GenericInteger",
                "GenericLong",
                "GenericFloat",
                "Double",
                "GenericBigInteger",
                "BigDecimal");
        assertMethod(target, "m2", byte.class, nonNumbers, "GenericByte", "long", "GenericByte", "Double");

        assertMethod(target, "m2", short.class, primitives, "long", "long", "long", "long", "double", "double");
        assertMethod(target,
                "m2",
                short.class,
                boxed,
                "GenericShort",
                "GenericShort",
                "GenericInteger",
                "GenericLong",
                "GenericFloat",
                "Double",
                "GenericBigInteger",
                "BigDecimal");
        assertMethod(target, "m2", short.class, nonNumbers, "GenericShort", "long", "GenericShort", "Double");

        assertMethod(target, "m2", int.class, primitives, "long", "long", "long", "long", "double", "double");
        assertMethod(target,
                "m2",
                int.class,
                boxed,
                "GenericInteger",
                "GenericInteger",
                "GenericInteger",
                "GenericLong",
                "GenericFloat",
                "Double",
                "GenericBigInteger",
                "BigDecimal");
        assertMethod(target, "m2", int.class, nonNumbers, "GenericInteger", "long", "GenericInteger", "GenericInteger");

        assertMethod(target, "m2", long.class, primitives, "long", "long", "long", "long", "double", "double");
        assertMethod(target,
                "m2",
                long.class,
                boxed,
                "GenericLong",
                "GenericLong",
                "GenericLong",
                "GenericLong",
                "GenericFloat",
                "Double",
                "GenericBigInteger",
                "BigDecimal");
        assertMethod(target, "m2", long.class, nonNumbers, "GenericLong", "long", "GenericLong", "GenericLong");

        assertMethod(target, "m2", float.class, primitives, "double", "double", "double", "double", "double", "double");
        assertMethod(target,
                "m2",
                float.class,
                boxed,
                "GenericFloat",
                "GenericFloat",
                "GenericFloat",
                "GenericFloat",
                "GenericFloat",
                "Double",
                "BigDecimal",
                "BigDecimal");
        assertMethod(target, "m2", float.class, nonNumbers, "GenericFloat", "double", "GenericFloat", "GenericFloat");

        assertMethod(target,
                "m2",
                double.class,
                primitives,
                "double",
                "double",
                "double",
                "double",
                "double",
                "double");
        assertMethod(target,
                "m2",
                double.class,
                boxed,
                "Double",
                "Double",
                "Double",
                "Double",
                "Double",
                "Double",
                "BigDecimal",
                "BigDecimal");
        assertMethod(target, "m2", double.class, nonNumbers, "GenericDouble", "double", "GenericDouble", "Double");
    }

    static Stream<Arguments> testVarArguments() {
        return Stream.of(
                arguments("Integer", "vararg", new Class<?>[]{Integer.class}),
                arguments("Integer", "vararg", new Class<?>[]{int.class}),
                arguments("Integer", "vararg", new Class<?>[]{Short.class}),
                arguments("Object[]", "vararg", new Class<?>[]{Short.class, Short.class}),
                arguments("Object[]", "vararg", new Class<?>[]{Short.class, Double.class}),
                arguments("Object[]", "vararg", new Class<?>[]{Double.class}),
                arguments("Object[]", "vararg", new Class<?>[]{String.class, Integer.class}),
                arguments("Object[]", "vararg", new Class<?>[]{int.class, int.class}),
                arguments("Object[]", "vararg", new Class<?>[]{Integer.class, double.class}),
                arguments("Object[]", "vararg", new Class<?>[]{Integer.class, int.class}),

                arguments("Object[]", "vararg1", new Class<?>[]{Integer.class}),
                arguments("Object[]", "vararg1", new Class<?>[]{String.class, Integer.class}),
                arguments("Object[]", "vararg1", new Class<?>[]{String.class, int.class}),

                arguments("Number...Number[]", "vararg2", new Class<?>[]{Integer.class}),
                arguments("Number...Number[]", "vararg2", new Class<?>[]{Double.class, Integer.class}),
                arguments("Generic...Object[]", "vararg2", new Class<?>[]{String.class, Integer.class}),
                arguments("Generic...Object[]", "vararg2", new Class<?>[]{String.class, int.class}),
                arguments("Generic...Integer[]", "vararg2", new Class<?>[]{Integer.class, int.class}),

                arguments("Integer", "vararg3", new Class<?>[]{Integer.class}),
                arguments("Integer", "vararg3", new Class<?>[]{int.class}),
                arguments("Object", "vararg3", new Class<?>[]{Short.class}),
                arguments("Number[]", "vararg3", new Class<?>[]{Short.class, Short.class}),
                arguments("Number[]", "vararg3", new Class<?>[]{Short.class, Double.class}),
                arguments("Object", "vararg3", new Class<?>[]{Double.class}),
                arguments(NF, "vararg3", new Class<?>[]{String.class, Integer.class}),
                arguments("Number[]", "vararg3", new Class<?>[]{Integer.class, int.class}),

                arguments("Generic_Comparable...Integer[]", "vararg4", new Class<?>[]{Integer.class}),
                arguments("Generic_Comparable...Integer[]", "vararg4", new Class<?>[]{int.class}),
                arguments("Generic_Comparable...Integer[]", "vararg4", new Class<?>[]{int.class, int.class}),
                arguments("Generic_Comparable...Integer[]", "vararg4", new Class<?>[]{Integer.class, int.class}),
                arguments("Generic_Comparable...Integer[]", "vararg4", new Class<?>[]{Integer[].class}),
                arguments("Generic_Comparable...Integer[]", "vararg4", new Class<?>[]{int[].class}),

                arguments("Long...Long[]", "vararg4", new Class<?>[]{Long.class}),
                arguments("Long...Long[]", "vararg4", new Class<?>[]{long.class}),
                arguments("Long...Long[]", "vararg4", new Class<?>[]{Long.class, long.class}),
                arguments("Long...Long[]", "vararg4", new Class<?>[]{long.class, long.class}),
                arguments("Long...Long[]", "vararg4", new Class<?>[]{Long[].class}),
                arguments("Long...Long[]", "vararg4", new Class<?>[]{long[].class}),

                arguments("Generic_Comparable...Double[]", "vararg4", new Class<?>[]{Double.class}),
                arguments("Generic_Comparable...Double[]", "vararg4", new Class<?>[]{double.class}),
                arguments("Generic_Comparable...Double[]", "vararg4", new Class<?>[]{double.class, double.class}),
                arguments("Generic_Comparable...Double[]", "vararg4", new Class<?>[]{Double.class, double.class}),
                arguments("Generic_Comparable...Double[]", "vararg4", new Class<?>[]{Double[].class}),
                arguments("Generic_Comparable...Double[]", "vararg4", new Class<?>[]{double[].class}),

                arguments("Generic_Comparable...String[]", "vararg4", new Class<?>[]{String.class}),
                arguments("Generic_Comparable...String[]", "vararg4", new Class<?>[]{String.class, String.class}),
                arguments("Generic_Comparable...String[]", "vararg4", new Class<?>[]{String[].class}),
                arguments("Generic...List[]", "vararg4", new Class<?>[]{List.class}),
                arguments("Generic...List[]", "vararg4", new Class<?>[]{List.class, List.class}),
                arguments("Generic...List[]", "vararg4", new Class<?>[]{List[].class}),

                arguments("Generic...Object[]", "vararg4", new Class<?>[]{String.class, Integer.class}),
                arguments("BigDecimal...BigDecimal[]", "vararg4", new Class<?>[]{Integer.class, double.class}));
    }

    @ParameterizedTest
    @MethodSource
    void testVarArguments(Object expected, String methodName, Class<?>[] argTypes)
            throws AmbiguousMethodException {
        assertMethod(expected, target, methodName, argTypes);
    }

    @Test
    void testGenerics() throws AmbiguousMethodException {
        assertMethod("Collection", target, "gen", Set.class);
        assertMethod("GenericInteger", target, "gen", int.class);
        assertMethod("Collection", target, "gen", Collection.class);
        assertMethod("GenList", target, "gen", List.class);
        assertMethod("AbstractList", target, "gen", ArrayList.class);
        assertMethod("GenericHashMap", target, "gen", HashMap.class);
        assertMethod("Collection", target, "gen", ConcurrentLinkedQueue.class);
        assertMethod("DequeArrayDeque", target, "gen", ArrayDeque.class);
        assertAmbiguous(target, "gen", LinkedList.class);
    }

    @Test
    void testGenericsVararg() throws AmbiguousMethodException {
        assertMethod(target,
                "singleGenVararg",
                primitives,
                "Byte[]",
                "Short[]",
                "Integer[]",
                "Long[]",
                "Float[]",
                "Double[]");
        assertMethod(target,
                "singleGenVararg",
                boxed,
                "Byte[]",
                "Short[]",
                "Integer[]",
                "Long[]",
                "Float[]",
                "Double[]",
                "BigInteger[]",
                "BigDecimal[]");
        assertMethod(target, "singleGenVararg", nonNumbers, "Boolean[]", "Character[]", "Boolean[]", "Character[]");
        assertMethod(target,
                "singleGenVararg",
                primitivesArray,
                "Byte[]",
                "Short[]",
                "Integer[]",
                "Long[]",
                "Float[]",
                "Double[]");
        assertMethod(target,
                "singleGenVararg",
                boxedArray,
                "Byte[]",
                "Short[]",
                "Integer[]",
                "Long[]",
                "Float[]",
                "Double[]",
                "BigInteger[]",
                "BigDecimal[]");
        assertMethod(target,
                "singleGenVararg",
                nonNumbersArray,
                "Boolean[]",
                "Character[]",
                "Boolean[]",
                "Character[]");
        assertNotFound(target, "singleGenVararg", List.class);
    }
}
