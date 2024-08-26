package org.openl.rules.convertor;

import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;

public class ObjectToDataOpenCastConvertor {

    private ICastFactory castFactory = null;

    private ICastFactory getCastFactory() {
        if (castFactory == null) {
            IOpenBinder binder = OpenL.getInstance(OpenL.OPENL_JAVA_NAME).getBinder();
            castFactory = binder.getCastFactory();
        }
        return castFactory;
    }

    public IOpenCast getConvertor(Class<?> fromClass, Class<?> toClass) {
        return getCastFactory().getCast(JavaOpenClass.getOpenClass(fromClass), JavaOpenClass.getOpenClass(toClass));
    }

    public Object convert(Object from, IOpenClass toClass) {
        if (from == null) {
            return toClass.nullObject();
        }
        var fromClass = from.getClass();
        if (ClassUtils.isAssignable(fromClass, toClass.getInstanceClass())) {
            return from;
        }
        var cast = getCastFactory().getCast(JavaOpenClass.getOpenClass(fromClass), toClass);

        if (cast != null && cast.isImplicit()) {
            return cast.convert(from);
        }

        throw new ClassCastException("Cannot convert " + fromClass + " to " + toClass);
    }
}
