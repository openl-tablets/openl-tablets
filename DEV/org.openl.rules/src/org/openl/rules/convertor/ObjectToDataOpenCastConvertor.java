package org.openl.rules.convertor;

import java.util.HashMap;
import java.util.Map;

import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.JavaNoCast;
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

    private static Map<ClassCastPair, IOpenCast> convertors = new HashMap<ClassCastPair, IOpenCast>();
    
    public static synchronized IOpenCast getConvertor(Class<?> toClass, Class<?> fromClass) {
        if (toClass == fromClass)
            return new JavaNoCast();
        ClassCastPair pair = new ClassCastPair(fromClass, toClass);
        if (convertors.containsKey(pair)){
            return convertors.get(pair);
        }
        
        IOpenBinder binder = OpenL.getInstance(OpenL.OPENL_JAVA_NAME).getBinder();
        IOpenCast openCast = binder.getCastFactory().getCast(JavaOpenClass.getOpenClass(fromClass), JavaOpenClass.getOpenClass(toClass));
        convertors.put(pair, openCast);
        
        return openCast;
    }
}
