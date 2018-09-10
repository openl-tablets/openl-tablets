package org.openl.rules.convertor;

import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.java.JavaOpenClass;

public class ObjectToDataOpenCastConvertor {

    private ICastFactory castFactory = null;

    private ICastFactory getCastFactory() {
        if (castFactory == null) {
            IOpenBinder binder = OpenL.getInstance(OpenL.OPENL_JAVA_NAME).getBinder();
            castFactory = binder.getCastFactory();
        }
        return castFactory;
    }

    public IOpenCast getConvertor(Class<?> toClass, Class<?> fromClass) {
        return getCastFactory().getCast(JavaOpenClass.getOpenClass(fromClass), JavaOpenClass.getOpenClass(toClass));
    }
}

