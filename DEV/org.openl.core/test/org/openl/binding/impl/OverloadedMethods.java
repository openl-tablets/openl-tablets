package org.openl.binding.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;

public class OverloadedMethods {
    public static String _byte(byte arg) {
        return "byte";
    }

    public static String _short(short arg) {
        return "short";
    }

    public static String _int(int arg) {
        return "int";
    }

    public static String _long(long arg) {
        return "long";
    }

    public static String _float(float arg) {
        return "float";
    }

    public static String _double(double arg) {
        return "double";
    }

    public static String _Byte(Byte arg) {
        return "Byte";
    }

    public static String _Short(Short arg) {
        return "Short";
    }

    public static String _Integer(Integer arg) {
        return "Integer";
    }

    public static String _Long(Long arg) {
        return "Long";
    }

    public static String _Float(Float arg) {
        return "Float";
    }

    public static String _Double(Double arg) {
        return "Double";
    }

    public static String _BigInteger(BigInteger arg) {
        return "BigInteger";
    }

    public static String _BigDecimal(BigDecimal arg) {
        return "BigDecimal";
    }

    public static String _boolean(boolean arg) {
        return "boolean";
    }

    public static String _char(char arg) {
        return "char";
    }

    public static String _Boolean(Boolean arg) {
        return "Boolean";
    }

    public static String _Character(Character arg) {
        return "Character";
    }

    public static String _ByteValue(ByteValue arg) {
        return "ByteValue";
    }

    public static String _ShortValue(ShortValue arg) {
        return "ShortValue";
    }

    public static String _IntValue(IntValue arg) {
        return "IntValue";
    }

    public static String _LongValue(LongValue arg) {
        return "LongValue";
    }

    public static String _FloatValue(FloatValue arg) {
        return "FloatValue";
    }

    public static String _DoubleValue(DoubleValue arg) {
        return "DoubleValue";
    }

    public static String _BigIntegerValue(BigIntegerValue arg) {
        return "BigIntegerValue";
    }

    public static String _BigDecimalValue(BigDecimalValue arg) {
        return "BigDecimalValue";
    }

    public static String m0_prim(int arg) {
        return "int";
    }

    public static String m0_prim(float arg) {
        return "float";
    }

    public static String m0_Boxed(Integer arg) {
        return "Integer";
    }

    public static String m0_Boxed(Float arg) {
        return "Float";
    }

    public static String m0_mixed(int arg) {
        return "int";
    }

    public static String m0_mixed(float arg) {
        return "float";
    }

    public static String m0_mixed(Short arg) {
        return "Short";
    }

    public static String m0_mixed(BigDecimal arg) {
        return "BigDecimal";
    }

    public static String m0_comp(short arg) {
        return "short";
    }

    public static String m0_comp(Long arg) {
        return "Long";
    }

    public static String m0_comp(Comparable<?> arg) {
        return "Comparable";
    }

    public static String m1(byte arg) {
        return "byte";
    }

    public static String m1(short arg) {
        return "short";
    }

    public static String m1(int arg) {
        return "int";
    }

    public static String m1(long arg) {
        return "long";
    }

    public static String m1(float arg) {
        return "float";
    }

    public static String m1(double arg) {
        return "double";
    }

    public static String m1(Byte arg) {
        return "Byte";
    }

    public static String m1(Short arg) {
        return "Short";
    }

    public static String m1(Integer arg) {
        return "Integer";
    }

    public static String m1(Long arg) {
        return "Long";
    }

    public static String m1(Float arg) {
        return "Float";
    }

    public static String m1(Double arg) {
        return "Double";
    }

    public static String m1(BigInteger arg) {
        return "BigInteger";
    }

    public static String m1(BigDecimal arg) {
        return "BigDecimal";
    }

    public static String m1(boolean arg) {
        return "boolean";
    }

    public static String m1(char arg) {
        return "char";
    }

    public static String m1(Boolean arg) {
        return "Boolean";
    }

    public static String m1(Character arg) {
        return "Character";
    }

    public static String m1(ByteValue arg) {
        return "ByteValue";
    }

    public static String m1(ShortValue arg) {
        return "ShortValue";
    }

    public static String m1(IntValue arg) {
        return "IntValue";
    }

    public static String m1(LongValue arg) {
        return "LongValue";
    }

    public static String m1(FloatValue arg) {
        return "FloatValue";
    }

    public static String m1(DoubleValue arg) {
        return "DoubleValue";
    }

    public static String m1(BigIntegerValue arg) {
        return "BigIntegerValue";
    }

    public static String m1(BigDecimalValue arg) {
        return "BigDecimalValue";
    }

    public static <T> String m2(T arg1, T arg2) {
        return "Generic" + (arg1 != null ? arg1.getClass().getSimpleName() : "");
    }

    public static <T> String m2(BigDecimal arg1, BigDecimal arg2) {
        return "BigDecimal";
    }

    public static <T> String m2(Double arg1, Double arg2) {
        return "Double";
    }

    public static <T> String m2(double arg1, double arg2) {
        return "double";
    }

    public static <T> String m2(long arg1, long arg2) {
        return "long";
    }

    public static <T> String gen(T arg1) {
        return "Generic" + (arg1 != null ? arg1.getClass().getSimpleName() : "");
    }

    public static <T extends List> String gen(T arg1) {
        return "GenList" + (arg1 != null ? arg1.getClass().getSimpleName() : "");
    }

    public static <T extends Deque> String gen(T arg1) {
        return "Deque" + (arg1 != null ? arg1.getClass().getSimpleName() : "");
    }

    public static String gen(Collection arg1) {
        return "Collection";
    }

    public static String gen(AbstractList arg1) {
        return "AbstractList";
    }

    public static String vararg(Object... args) {
        return args.getClass().getSimpleName();
    }

    public static String vararg(Integer args) {
        return "Integer";
    }

    public static String vararg1(Object... args) {
        return args.getClass().getSimpleName();
    }

    public static String vararg2(Number... args) {
        return "Number..." + args.getClass().getSimpleName();
    }

    public static <T> String vararg2(T... args) {
        return "Generic..." + args.getClass().getSimpleName();
    }

    public static String vararg3(Number... args) {
        return args.getClass().getSimpleName();
    }

    public static String vararg3(Object args) {
        return "Object";
    }

    public static String vararg3(Integer args) {
        return "Integer";
    }

    public static <T> String vararg4(T... args) {
        return "Generic..." + args.getClass().getSimpleName();
    }

    public static <T extends Comparable<T>> String vararg4(T... args) {
        return "Generic_Comparable..." + args.getClass().getSimpleName();
    }

    public static String vararg4(DoubleValue... args) {
        return "DoubleValue..." + args.getClass().getSimpleName();
    }

    public static String vararg4(Long... args) {
        return "Long..." + args.getClass().getSimpleName();
    }
}
