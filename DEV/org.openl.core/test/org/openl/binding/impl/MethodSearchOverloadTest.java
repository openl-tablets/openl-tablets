package org.openl.binding.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.Test;
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
 *
 * The order of boxing:
 * <ul>
 * <li>primitives -> boxed -> valued</li>
 * <li>boxed -> superClasses</li>
 * <li>valued -> superClasses</li>
 * <li>boxed -> primitives</li>
 * </ul>
 *
 */
public class MethodSearchOverloadTest extends AbstractMethodSearchTest {
    private static final Class<?> target = OverloadedMethods.class;
    private static final Class<?>[] primitives = new Class[] { byte.class,
            short.class,
            int.class,
            long.class,
            float.class,
            double.class };
    private static final Class<?>[] boxed = new Class[] { Byte.class,
            Short.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            BigInteger.class,
            BigDecimal.class };
    private static final Class<?>[] nonNumbers = new Class[] { boolean.class,
            char.class,
            Boolean.class,
            Character.class };
    private static final Class<?>[] primitivesArray = new Class[] { byte[].class,
            short[].class,
            int[].class,
            long[].class,
            float[].class,
            double[].class };
    private static final Class<?>[] boxedArray = new Class[] { Byte[].class,
            Short[].class,
            Integer[].class,
            Long[].class,
            Float[].class,
            Double[].class,
            BigInteger[].class,
            BigDecimal[].class };
    private static final Class<?>[] nonNumbersArray = new Class[] { boolean[].class,
            char[].class,
            Boolean[].class,
            Character[].class };

    @Test
    public void testSearch() throws AmbiguousMethodException {
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
            "int",
            "BigDecimal",
            "float",
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
    public void testExpandPrimitives() throws AmbiguousMethodException {
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

    @Test
    public void testExpandBoxed() throws AmbiguousMethodException {
        assertMethod(target, "_Byte", primitives, "Byte", NF, NF, NF, NF, NF);
        assertMethod(target, "_Byte", boxed, "Byte", NF, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_Byte", nonNumbers, NF, NF, NF, NF);

        assertMethod(target, "_Short", primitives, "Short", "Short", NF, NF, NF, NF);
        assertMethod(target, "_Short", boxed, "Short", "Short", NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_Short", nonNumbers, NF, NF, NF, NF);

        assertMethod(target, "_Integer", primitives, "Integer", "Integer", "Integer", NF, NF, NF);
        assertMethod(target, "_Integer", boxed, "Integer", "Integer", "Integer", NF, NF, NF, NF, NF);
        assertMethod(target, "_Integer", nonNumbers, NF, "Integer", NF, "Integer");

        assertMethod(target, "_Long", primitives, "Long", "Long", "Long", "Long", NF, NF);
        assertMethod(target, "_Long", boxed, "Long", "Long", "Long", "Long", NF, NF, NF, NF);
        assertMethod(target, "_Long", nonNumbers, NF, "Long", NF, "Long");

        assertMethod(target, "_Float", primitives, "Float", "Float", "Float", "Float", "Float", NF);
        assertMethod(target, "_Float", boxed, "Float", "Float", "Float", "Float", "Float", NF, NF, NF);
        assertMethod(target, "_Float", nonNumbers, NF, "Float", NF, "Float");

        assertMethod(target, "_Double", primitives, "Double", "Double", "Double", "Double", "Double", "Double");
        assertMethod(target, "_Double", boxed, "Double", "Double", "Double", "Double", "Double", "Double", NF, NF);
        assertMethod(target, "_Double", nonNumbers, NF, "Double", NF, "Double");

        assertMethod(target, "_Boolean", primitives, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_Boolean", boxed, NF, NF, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_Boolean", nonNumbers, "Boolean", NF, "Boolean", NF);

        assertMethod(target, "_Character", primitives, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_Character", boxed, NF, NF, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_Character", nonNumbers, NF, "Character", NF, "Character");

        assertMethod(target, "_BigInteger", primitives, "BigInteger", "BigInteger", "BigInteger", "BigInteger", NF, NF);
        assertMethod(target,
            "_BigInteger",
            boxed,
            "BigInteger",
            "BigInteger",
            "BigInteger",
            "BigInteger",
            NF,
            NF,
            "BigInteger",
            NF);
        assertMethod(target, "_BigInteger", nonNumbers, NF, "BigInteger", NF, "BigInteger");

        assertMethod(target,
            "_BigDecimal",
            primitives,
            "BigDecimal",
            "BigDecimal",
            "BigDecimal",
            "BigDecimal",
            "BigDecimal",
            "BigDecimal");
        assertMethod(target,
            "_BigDecimal",
            boxed,
            "BigDecimal",
            "BigDecimal",
            "BigDecimal",
            "BigDecimal",
            "BigDecimal",
            "BigDecimal",
            "BigDecimal",
            "BigDecimal");
        assertMethod(target, "_BigDecimal", nonNumbers, NF, "BigDecimal", NF, "BigDecimal");
    }

    @Test
    public void testOneArgument() throws AmbiguousMethodException {
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
    public void testOneArgument2() throws AmbiguousMethodException {
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
    public void testTwoArguments() throws AmbiguousMethodException {
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

    @Test
    public void testVarArguments() throws AmbiguousMethodException {
        assertMethod("Integer", target, "vararg", Integer.class);
        assertMethod("Integer", target, "vararg", int.class);
        assertMethod("Integer", target, "vararg", Short.class);
        assertMethod("Short[]", target, "vararg", Short.class, Short.class);
        assertMethod("Number[]", target, "vararg", Short.class, Double.class);
        assertMethod("Double[]", target, "vararg", Double.class);
        assertMethod("Object[]", target, "vararg", String.class, Integer.class);
        assertMethod("Object[]", target, "vararg", int.class, int.class);
        assertMethod("Number[]", target, "vararg", Integer.class, double.class);
        assertMethod("Integer[]", target, "vararg", Integer.class, int.class);

        assertMethod("Integer[]", target, "vararg1", Integer.class);
        assertMethod("Object[]", target, "vararg1", String.class, Integer.class);
        assertMethod("Object[]", target, "vararg1", String.class, int.class);

        assertMethod("Number...Integer[]", target, "vararg2", Integer.class);
        assertMethod("Number...Number[]", target, "vararg2", Double.class, Integer.class);
        assertMethod("Generic...Object[]", target, "vararg2", String.class, Integer.class);
        assertMethod("Generic...Object[]", target, "vararg2", String.class, int.class);
        assertMethod("Number...Integer[]", target, "vararg2", Integer.class, int.class);

        assertMethod("Integer", target, "vararg3", Integer.class);
        assertMethod("Integer", target, "vararg3", int.class);
        assertMethod("Object", target, "vararg3", Short.class);
        assertMethod("Short[]", target, "vararg3", Short.class, Short.class);
        assertMethod("Number[]", target, "vararg3", Short.class, Double.class);
        assertMethod("Object", target, "vararg3", Double.class);
        assertNotFound(target, "vararg3", String.class, Integer.class);
        assertMethod("Integer[]", target, "vararg3", Integer.class, int.class);

        assertMethod("Generic_Comparable...Integer[]", target, "vararg4", Integer.class);
        assertMethod("Generic_Comparable...Integer[]", target, "vararg4", int.class);
        assertMethod("Generic_Comparable...Integer[]", target, "vararg4", int.class, int.class);
        assertMethod("Generic_Comparable...Integer[]", target, "vararg4", Integer.class, int.class);
        assertMethod("Generic_Comparable...Integer[]", target, "vararg4", Integer[].class);
        assertMethod("Generic_Comparable...Integer[]", target, "vararg4", int[].class);

        assertMethod("Long...Long[]", target, "vararg4", Long.class);
        assertMethod("Long...Long[]", target, "vararg4", long.class);
        assertMethod("Long...Long[]", target, "vararg4", Long.class, long.class);
        assertMethod("Long...Long[]", target, "vararg4", long.class, long.class);
        assertMethod("Long...Long[]", target, "vararg4", Long[].class);
        assertMethod("Long...Long[]", target, "vararg4", long[].class);

        assertMethod("Generic_Comparable...Double[]", target, "vararg4", Double.class);
        assertMethod("Generic_Comparable...Double[]", target, "vararg4", double.class);
        assertMethod("Generic_Comparable...Double[]", target, "vararg4", double.class, double.class);
        assertMethod("Generic_Comparable...Double[]", target, "vararg4", Double.class, double.class);
        assertMethod("Generic_Comparable...Double[]", target, "vararg4", Double[].class);
        assertMethod("Generic_Comparable...Double[]", target, "vararg4", double[].class);

        assertMethod("Generic_Comparable...String[]", target, "vararg4", String.class);
        assertMethod("Generic_Comparable...String[]", target, "vararg4", String.class, String.class);
        assertMethod("Generic_Comparable...String[]", target, "vararg4", String[].class);
        assertMethod("Generic...List[]", target, "vararg4", List.class);
        assertMethod("Generic...List[]", target, "vararg4", List.class, List.class);
        assertMethod("Generic...List[]", target, "vararg4", List[].class);

        assertMethod("Generic...Object[]", target, "vararg4", String.class, Integer.class);
        assertMethod("Generic...Number[]", target, "vararg4", Integer.class, double.class);
    }

    @Test
    public void testGenerics() throws AmbiguousMethodException {
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
    public void testGenericsVararg() throws AmbiguousMethodException {
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
