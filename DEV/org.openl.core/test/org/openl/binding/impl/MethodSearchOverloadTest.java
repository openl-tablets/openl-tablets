package org.openl.binding.impl;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Test;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;

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
    private static final Class<?>[] valued = new Class[] { ByteValue.class,
            ShortValue.class,
            IntValue.class,
            LongValue.class,
            FloatValue.class,
            DoubleValue.class,
            BigIntegerValue.class,
            BigDecimalValue.class };

    @Test
    public void testSearch() {
        assertMethod(target, "m0_prim", primitives, "int", "int", "int", "float", "float", NF);
        assertMethod(target, "m0_prim", boxed, "int", "int", "int", "float", "float", NF, NF, NF);
        assertMethod(target, "m0_prim", nonNumbers, NF, "int", NF, "int");
        assertMethod(target, "m0_prim", valued, NF, NF, NF, NF, NF, NF, NF, NF);

        assertMethod(target, "m0_Boxed", primitives, "Integer", "Integer", "Integer", "Float", "Float", NF);
        assertMethod(target, "m0_Boxed", boxed, "Integer", "Integer", "Integer", "Float", "Float", NF, NF, NF);
        assertMethod(target, "m0_Boxed", nonNumbers, NF, "Integer", NF, "Integer");
        assertMethod(target, "m0_Boxed", valued, NF, NF, NF, NF, NF, NF, NF, NF);

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
        assertMethod(target, "m0_mixed", valued, NF, NF, NF, NF, NF, NF, NF, NF);

        assertMethod(target, "m0_comp", primitives, "short", "short", "Long", "Long", "Comparable", "Comparable");
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
        assertMethod(target, "m0_comp", nonNumbers, "Comparable", "Long", "Comparable", "Comparable");
        assertMethod(target,
            "m0_comp",
            valued,
            "Comparable",
            "Comparable",
            "Comparable",
            "Comparable",
            "Comparable",
            "Comparable",
            "Comparable",
            "Comparable");
    }

    @Test
    public void testExpandPrimitives() {
        assertMethod(target, "_byte", primitives, "byte", NF, NF, NF, NF, NF);
        assertMethod(target, "_byte", boxed, "byte", NF, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_byte", nonNumbers, NF, NF, NF, NF);
        assertMethod(target, "_byte", valued, NF, NF, NF, NF, NF, NF, NF, NF);

        assertMethod(target, "_short", primitives, "short", "short", NF, NF, NF, NF);
        assertMethod(target, "_short", boxed, "short", "short", NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_short", nonNumbers, NF, NF, NF, NF);
        assertMethod(target, "_short", valued, NF, NF, NF, NF, NF, NF, NF, NF);

        assertMethod(target, "_int", primitives, "int", "int", "int", NF, NF, NF);
        assertMethod(target, "_int", boxed, "int", "int", "int", NF, NF, NF, NF, NF);
        assertMethod(target, "_int", nonNumbers, NF, "int", NF, "int");
        assertMethod(target, "_int", valued, NF, NF, NF, NF, NF, NF, NF, NF);

        assertMethod(target, "_long", primitives, "long", "long", "long", "long", NF, NF);
        assertMethod(target, "_long", boxed, "long", "long", "long", "long", NF, NF, NF, NF);
        assertMethod(target, "_long", nonNumbers, NF, "long", NF, "long");
        assertMethod(target, "_long", valued, NF, NF, NF, NF, NF, NF, NF, NF);

        assertMethod(target, "_float", primitives, "float", "float", "float", "float", "float", NF);
        assertMethod(target, "_float", boxed, "float", "float", "float", "float", "float", NF, NF, NF);
        assertMethod(target, "_float", nonNumbers, NF, "float", NF, "float");
        assertMethod(target, "_float", valued, NF, NF, NF, NF, NF, NF, NF, NF);

        assertMethod(target, "_double", primitives, "double", "double", "double", "double", "double", "double");
        assertMethod(target, "_double", boxed, "double", "double", "double", "double", "double", "double", NF, NF);
        assertMethod(target, "_double", nonNumbers, NF, "double", NF, "double");
        assertMethod(target, "_double", valued, NF, NF, NF, NF, NF, NF, NF, NF);

        assertMethod(target, "_boolean", primitives, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_boolean", boxed, NF, NF, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_boolean", nonNumbers, "boolean", NF, "boolean", NF);
        assertMethod(target, "_boolean", valued, NF, NF, NF, NF, NF, NF, NF, NF);

        assertMethod(target, "_char", primitives, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_char", boxed, NF, NF, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_char", nonNumbers, NF, "char", NF, "char");
        assertMethod(target, "_char", valued, NF, NF, NF, NF, NF, NF, NF, NF);
    }

    @Test
    public void testExpandBoxed() {
        assertMethod(target, "_Byte", primitives, "Byte", NF, NF, NF, NF, NF);
        assertMethod(target, "_Byte", boxed, "Byte", NF, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_Byte", nonNumbers, NF, NF, NF, NF);
        assertMethod(target, "_Byte", valued, NF, NF, NF, NF, NF, NF, NF, NF);

        assertMethod(target, "_Short", primitives, "Short", "Short", NF, NF, NF, NF);
        assertMethod(target, "_Short", boxed, "Short", "Short", NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_Short", nonNumbers, NF, NF, NF, NF);
        assertMethod(target, "_Short", valued, NF, NF, NF, NF, NF, NF, NF, NF);

        assertMethod(target, "_Integer", primitives, "Integer", "Integer", "Integer", NF, NF, NF);
        assertMethod(target, "_Integer", boxed, "Integer", "Integer", "Integer", NF, NF, NF, NF, NF);
        assertMethod(target, "_Integer", nonNumbers, NF, "Integer", NF, "Integer");
        assertMethod(target, "_Integer", valued, NF, NF, NF, NF, NF, NF, NF, NF);

        assertMethod(target, "_Long", primitives, "Long", "Long", "Long", "Long", NF, NF);
        assertMethod(target, "_Long", boxed, "Long", "Long", "Long", "Long", NF, NF, NF, NF);
        assertMethod(target, "_Long", nonNumbers, NF, "Long", NF, "Long");
        assertMethod(target, "_Long", valued, NF, NF, NF, NF, NF, NF, NF, NF);

        assertMethod(target, "_Float", primitives, "Float", "Float", "Float", "Float", "Float", NF);
        assertMethod(target, "_Float", boxed, "Float", "Float", "Float", "Float", "Float", NF, NF, NF);
        assertMethod(target, "_Float", nonNumbers, NF, "Float", NF, "Float");
        assertMethod(target, "_Float", valued, NF, NF, NF, NF, NF, NF, NF, NF);

        assertMethod(target, "_Double", primitives, "Double", "Double", "Double", "Double", "Double", "Double");
        assertMethod(target, "_Double", boxed, "Double", "Double", "Double", "Double", "Double", "Double", NF, NF);
        assertMethod(target, "_Double", nonNumbers, NF, "Double", NF, "Double");
        assertMethod(target, "_Double", valued, NF, NF, NF, NF, NF, NF, NF, NF);

        assertMethod(target, "_Boolean", primitives, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_Boolean", boxed, NF, NF, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_Boolean", nonNumbers, "Boolean", NF, "Boolean", NF);
        assertMethod(target, "_Boolean", valued, NF, NF, NF, NF, NF, NF, NF, NF);

        assertMethod(target, "_Character", primitives, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_Character", boxed, NF, NF, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_Character", nonNumbers, NF, "Character", NF, "Character");
        assertMethod(target, "_Character", valued, NF, NF, NF, NF, NF, NF, NF, NF);

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
        assertMethod(target, "_BigInteger", valued, NF, NF, NF, NF, NF, NF, NF, NF);

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
        assertMethod(target, "_BigDecimal", valued, NF, NF, NF, NF, NF, NF, NF, NF);
    }

    @Test
    public void testExpandValued() {
        assertMethod(target, "_ByteValue", primitives, "ByteValue", NF, NF, NF, NF, NF);
        assertMethod(target, "_ByteValue", boxed, "ByteValue", NF, NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_ByteValue", nonNumbers, NF, NF, NF, NF);
        assertMethod(target, "_ByteValue", valued, "ByteValue", NF, NF, NF, NF, NF, NF, NF);

        assertMethod(target, "_ShortValue", primitives, "ShortValue", "ShortValue", NF, NF, NF, NF);
        assertMethod(target, "_ShortValue", boxed, "ShortValue", "ShortValue", NF, NF, NF, NF, NF, NF);
        assertMethod(target, "_ShortValue", nonNumbers, NF, NF, NF, NF);
        assertMethod(target, "_ShortValue", valued, "ShortValue", "ShortValue", NF, NF, NF, NF, NF, NF);

        assertMethod(target, "_IntValue", primitives, "IntValue", "IntValue", "IntValue", NF, NF, NF);
        assertMethod(target, "_IntValue", boxed, "IntValue", "IntValue", "IntValue", NF, NF, NF, NF, NF);
        assertMethod(target, "_IntValue", nonNumbers, NF, "IntValue", NF, "IntValue");
        assertMethod(target, "_IntValue", valued, "IntValue", "IntValue", "IntValue", NF, NF, NF, NF, NF);

        assertMethod(target, "_LongValue", primitives, "LongValue", "LongValue", "LongValue", "LongValue", NF, NF);
        assertMethod(target, "_LongValue", boxed, "LongValue", "LongValue", "LongValue", "LongValue", NF, NF, NF, NF);
        assertMethod(target, "_LongValue", nonNumbers, NF, "LongValue", NF, "LongValue");
        assertMethod(target, "_LongValue", valued, "LongValue", "LongValue", "LongValue", "LongValue", NF, NF, NF, NF);

        assertMethod(target,
            "_FloatValue",
            primitives,
            "FloatValue",
            "FloatValue",
            "FloatValue",
            "FloatValue",
            "FloatValue",
            NF);
        assertMethod(target,
            "_FloatValue",
            boxed,
            "FloatValue",
            "FloatValue",
            "FloatValue",
            "FloatValue",
            "FloatValue",
            NF,
            NF,
            NF);
        assertMethod(target, "_FloatValue", nonNumbers, NF, "FloatValue", NF, "FloatValue");
        assertMethod(target,
            "_FloatValue",
            valued,
            "FloatValue",
            "FloatValue",
            "FloatValue",
            "FloatValue",
            "FloatValue",
            NF,
            NF,
            NF);

        assertMethod(target,
            "_DoubleValue",
            primitives,
            "DoubleValue",
            "DoubleValue",
            "DoubleValue",
            "DoubleValue",
            "DoubleValue",
            "DoubleValue");
        assertMethod(target,
            "_DoubleValue",
            boxed,
            "DoubleValue",
            "DoubleValue",
            "DoubleValue",
            "DoubleValue",
            "DoubleValue",
            "DoubleValue",
            NF,
            NF);
        assertMethod(target, "_DoubleValue", nonNumbers, NF, "DoubleValue", NF, "DoubleValue");
        assertMethod(target,
            "_DoubleValue",
            valued,
            "DoubleValue",
            "DoubleValue",
            "DoubleValue",
            "DoubleValue",
            "DoubleValue",
            "DoubleValue",
            NF,
            NF);

        assertMethod(target,
            "_BigIntegerValue",
            primitives,
            "BigIntegerValue",
            "BigIntegerValue",
            "BigIntegerValue",
            "BigIntegerValue",
            NF,
            NF);
        assertMethod(target,
            "_BigIntegerValue",
            boxed,
            "BigIntegerValue",
            "BigIntegerValue",
            "BigIntegerValue",
            "BigIntegerValue",
            NF,
            NF,
            "BigIntegerValue",
            NF);
        assertMethod(target, "_BigIntegerValue", nonNumbers, NF, "BigIntegerValue", NF, "BigIntegerValue");
        assertMethod(target,
            "_BigIntegerValue",
            valued,
            "BigIntegerValue",
            "BigIntegerValue",
            "BigIntegerValue",
            "BigIntegerValue",
            NF,
            NF,
            "BigIntegerValue",
            NF);

        assertMethod(target,
            "_BigDecimalValue",
            primitives,
            "BigDecimalValue",
            "BigDecimalValue",
            "BigDecimalValue",
            "BigDecimalValue",
            "BigDecimalValue",
            "BigDecimalValue");
        assertMethod(target,
            "_BigDecimalValue",
            boxed,
            "BigDecimalValue",
            "BigDecimalValue",
            "BigDecimalValue",
            "BigDecimalValue",
            "BigDecimalValue",
            "BigDecimalValue",
            "BigDecimalValue",
            "BigDecimalValue");
        assertMethod(target, "_BigDecimalValue", nonNumbers, NF, "BigDecimalValue", NF, "BigDecimalValue");
        assertMethod(target,
            "_BigDecimalValue",
            valued,
            "BigDecimalValue",
            "BigDecimalValue",
            "BigDecimalValue",
            "BigDecimalValue",
            "BigDecimalValue",
            "BigDecimalValue",
            "BigDecimalValue",
            "BigDecimalValue");
    }

    @Test
    public void testOneArgument() {
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
        assertMethod(target,
            "m1",
            valued,
            "ByteValue",
            "ShortValue",
            "IntValue",
            "LongValue",
            "FloatValue",
            "DoubleValue",
            "BigIntegerValue",
            "BigDecimalValue");
    }

    @Test
    public void testOneArgument2() {
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
        assertMethod(target,
            "m1",
            valued,
            "ByteValue",
            "ShortValue",
            "IntValue",
            "LongValue",
            "FloatValue",
            "DoubleValue",
            "BigIntegerValue",
            "BigDecimalValue");
    }

    @Test
    public void testTwoArguments() {
        assertMethod(target, "m2", byte.class, primitives, "long", "long", "long", "long", "double", "double");
        assertMethod(target, "m2", byte.class, boxed, "GenericByte", "GenericShort", "GenericInteger", "GenericLong", "GenericFloat", "Double", "GenericBigInteger", "BigDecimal");
        //assertMethod(target, "m2", byte.class, nonNumbers, NF, "long", NF, "GenericInteger");
        assertMethod(target, "m2", byte.class, valued, "GenericByteValue", "GenericShortValue", "GenericIntValue", "GenericLongValue", "GenericFloatValue", "GenericDoubleValue","GenericBigIntegerValue","GenericBigDecimalValue");

        assertMethod(target, "m2", short.class, primitives, "long", "long", "long", "long", "double", "double");
        assertMethod(target, "m2", short.class, boxed, "GenericShort", "GenericShort", "GenericInteger", "GenericLong", "GenericFloat", "Double", "GenericBigInteger", "BigDecimal");
        //assertMethod(target, "m2", short.class, nonNumbers, NF, "long", NF, "GenericInteger");
        //assertMethod(target, "m2", short.class, valued, "GenericShortValue", "GenericShortValue", "GenericIntValue", "GenericLongValue", "GenericFloatValue", "GenericDoubleValue","GenericBigIntegerValue","GenericBigDecimalValue");

        assertMethod(target, "m2", int.class, primitives, "long", "long", "long", "long", "double", "double");
        assertMethod(target, "m2", long.class, primitives, "long", "long", "long", "long", "double", "double");
        assertMethod(target, "m2", float.class, primitives, "double", "double", "double", "double", "double", "double");
        assertMethod(target, "m2", double.class, primitives, "double", "double", "double", "double", "double", "double");
    }
}
