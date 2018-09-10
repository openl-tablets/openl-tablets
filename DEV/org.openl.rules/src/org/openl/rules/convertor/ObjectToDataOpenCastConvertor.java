package org.openl.rules.convertor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.JavaNoCast;
import org.openl.rules.enumeration.CaProvincesEnum;
import org.openl.rules.enumeration.CaRegionsEnum;
import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.enumeration.CurrenciesEnum;
import org.openl.rules.enumeration.LanguagesEnum;
import org.openl.rules.enumeration.RegionsEnum;
import org.openl.rules.enumeration.UsRegionsEnum;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

public class ObjectToDataOpenCastConvertor {

    public static class ClassCastPair {
        private Class<?> from;
        private Class<?> to;

        public ClassCastPair(Class<?> from, Class<?> to) {
            this.from = from;
            this.to = to;
        }

        public Class<?> getFrom() {
            return from;
        }

        public Class<?> getTo() {
            return to;
        }

        @Override
        public int hashCode() {
            return to.hashCode() + from.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ClassCastPair)) {
                return false;
            }
            ClassCastPair pair = (ClassCastPair) obj;
            return from == pair.from && to == pair.to;
        }
    }

    private static Map<ClassCastPair, IOpenCast> convertors = new ConcurrentHashMap<>();

    static {
        EnumToStringOpenCast enumToStringOpenCast = new EnumToStringOpenCast();
        convertors.put(new ClassCastPair(UsStatesEnum.class, String.class), enumToStringOpenCast);
        convertors.put(new ClassCastPair(CountriesEnum.class, String.class), enumToStringOpenCast);
        convertors.put(new ClassCastPair(UsRegionsEnum.class, String.class), enumToStringOpenCast);
        convertors.put(new ClassCastPair(CurrenciesEnum.class, String.class), enumToStringOpenCast);
        convertors.put(new ClassCastPair(LanguagesEnum.class, String.class), enumToStringOpenCast);
        convertors.put(new ClassCastPair(RegionsEnum.class, String.class), enumToStringOpenCast);
        convertors.put(new ClassCastPair(CaProvincesEnum.class, String.class), enumToStringOpenCast);
        convertors.put(new ClassCastPair(CaRegionsEnum.class, String.class), enumToStringOpenCast);

        convertors.put(new ClassCastPair(String.class, UsStatesEnum.class),
            new StringToEnumOpenCast(UsStatesEnum.class));
        convertors.put(new ClassCastPair(String.class, CountriesEnum.class),
            new StringToEnumOpenCast(CountriesEnum.class));
        convertors.put(new ClassCastPair(String.class, UsRegionsEnum.class),
            new StringToEnumOpenCast(UsRegionsEnum.class));
        convertors.put(new ClassCastPair(String.class, CurrenciesEnum.class),
            new StringToEnumOpenCast(CurrenciesEnum.class));
        convertors.put(new ClassCastPair(String.class, LanguagesEnum.class),
            new StringToEnumOpenCast(LanguagesEnum.class));
        convertors.put(new ClassCastPair(String.class, RegionsEnum.class), new StringToEnumOpenCast(RegionsEnum.class));
        convertors.put(new ClassCastPair(String.class, CaProvincesEnum.class),
            new StringToEnumOpenCast(CaProvincesEnum.class));
        convertors.put(new ClassCastPair(String.class, CaRegionsEnum.class),
            new StringToEnumOpenCast(CaRegionsEnum.class));

    }

    public static class StringToEnumOpenCast implements IOpenCast {
        @SuppressWarnings("rawtypes")
        private Class enumType;

        public StringToEnumOpenCast(Class<?> enumType) {
            this.enumType = enumType;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object convert(Object from) {
            return Enum.valueOf(enumType, (String) from);
        }

        @Override
        public int getDistance(IOpenClass from, IOpenClass to) {
            return 12;
        }

        @Override
        public boolean isImplicit() {
            return false;
        }
    }

    public static class EnumToStringOpenCast implements IOpenCast {
        @Override
        public Object convert(Object from) {
            return from.toString();
        }

        @Override
        public int getDistance(IOpenClass from, IOpenClass to) {
            return CastFactory.ENUM_TO_STRING_CAST_DISTANCE;
        }

        @Override
        public boolean isImplicit() {
            return false;
        }
    }

    private static ICastFactory castFactory = null; 
    
    private static ICastFactory getCastFactory() {
        if (castFactory == null) {
            IOpenBinder binder = OpenL.getInstance(OpenL.OPENL_JAVA_NAME).getBinder();
            castFactory = binder.getCastFactory();
        }
        return castFactory;
    }

    public static IOpenCast getConvertor(Class<?> toClass, Class<?> fromClass) {
        if (toClass == fromClass)
            return new JavaNoCast();
        ClassCastPair pair = new ClassCastPair(fromClass, toClass);
        IOpenCast cast = convertors.get(pair);
        if (cast != null) {
            return cast;
        }

        return getCastFactory().getCast(JavaOpenClass.getOpenClass(fromClass), JavaOpenClass.getOpenClass(toClass));
    }
}
