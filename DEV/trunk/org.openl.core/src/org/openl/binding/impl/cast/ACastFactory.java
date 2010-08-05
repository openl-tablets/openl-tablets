/*
 * Created on Jun 5, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.cast;

import java.util.HashMap;

import org.apache.commons.lang.ClassUtils;
import org.openl.binding.ICastFactory;
import org.openl.binding.IMethodFactory;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.cache.CacheUtils;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 * 
 */
public class ACastFactory implements ICastFactory {

    private static final JavaDownCast JAVA_DOWNCAST = new JavaDownCast();
    private static final JavaUpCast JAVA_UPCAST = new JavaUpCast();

    private static final IOpenCast NO_CAST = new IOpenCast() {
        public Object convert(Object from) {
            throw new UnsupportedOperationException();
        }

        public int getDistance(IOpenClass from, IOpenClass to) {
            throw new UnsupportedOperationException();
        }

        public boolean isImplicit() {
            throw new UnsupportedOperationException();
        }
    };

    private IMethodFactory methodFactory;

    private HashMap<Object, IOpenCast> castCache = new HashMap<Object, IOpenCast>();

    public ACastFactory() {
    }

    public ACastFactory(IMethodFactory methodFactory) {
        this.methodFactory = methodFactory;
    }

    public IMethodFactory getMethodFactory() {
        return methodFactory;
    }

    public void setMethodFactory(IMethodFactory factory) {
        methodFactory = factory;
    }

    public IOpenCast findCast(IOpenClass from, IOpenClass to) {

        if (to == NullOpenClass.the) {
            return null;
        } else if (from == NullOpenClass.the || from == JavaOpenClass.OBJECT) {
            return to.getInstanceClass().isPrimitive() ? null : JAVA_DOWNCAST;
        }

        if (to instanceof JavaOpenClass) {
            Class<?> fromClass = from.getInstanceClass();
            Class<?> toClass = to.getInstanceClass();

            if (toClass.isAssignableFrom(fromClass)) {
                return JAVA_DOWNCAST;
            }

            if (allowJavaUpcast(fromClass, toClass)) {
                return JAVA_UPCAST;
            }
        }

        IOpenCast typeCast = findCast(from, to, methodFactory);
        
        if (typeCast == NO_CAST) {
            typeCast = findCast(from, to, from);
        }
        if (typeCast == NO_CAST) {
            typeCast = findCast(from, to, to);
        }

        return typeCast;
    }

    public IOpenCast findCast(IOpenClass from, IOpenClass to, IMethodFactory methodFactory) {

        boolean auto = true;
        int distance = 1;
        IMethodCaller castCaller = null;

        try {
            castCaller = methodFactory.getMatchingMethod("autocast", new IOpenClass[] { from, to });
            
            Class<?> primitiveClass = ClassUtils.wrapperToPrimitive(from.getInstanceClass());
            
            if (castCaller == null && primitiveClass != null) {
                distance = 2;
                IOpenClass wrapperOpenClass = JavaOpenClass.getOpenClass(primitiveClass);
                castCaller = methodFactory.getMatchingMethod("autocast", new IOpenClass[] { wrapperOpenClass, to });
            }
            
        } catch (AmbiguousMethodException ex) {
        }

        if (castCaller == null) {
            auto = false;
            distance = 3;
            try {
                castCaller = methodFactory.getMatchingMethod("cast", new IOpenClass[] { from, to });
            } catch (AmbiguousMethodException ex) {
            }
        }

        if (castCaller == null) {
            return NO_CAST;
        }

        IMethodCaller distanceCaller = null;
        
        try {
            distanceCaller = methodFactory.getMatchingMethod("distance", new IOpenClass[] { from, to });
        } catch (AmbiguousMethodException ex) {
        }

        if (distanceCaller != null) {
            distance = ((Integer) distanceCaller.invoke(null, new Object[] { from.nullObject(), to.nullObject() }, null)).intValue();
        }

        return new MethodBasedCast(castCaller, auto, distance, to.nullObject());
    }

    public synchronized IOpenCast getCast(IOpenClass from, IOpenClass to) {
        
        Object key = CacheUtils.makeKey(from, to);

        IOpenCast cast = castCache.get(key);

        if (cast == null) {
            cast = findCast(from, to);
            castCache.put(key, cast);
        }

        if (cast != NO_CAST) {
            return cast;
        }
        
        return null;
    }

    /**
     * The following conversions are called the narrowing reference conversions:
     * 
     * From any class type S to any class type T, provided that S is a
     * superclass of T. (An important special case is that there is a narrowing
     * conversion from the class type Object to any other class type.) From any
     * class type S to any interface type K, provided that S is not final and
     * does not implement K. (An important special case is that there is a
     * narrowing conversion from the class type Object to any interface type.)
     * From type Object to any array type. From type Object to any interface
     * type. From any interface type J to any class type T that is not final.
     * From any interface type J to any class type T that is final, provided
     * that T implements J. From any interface type J to any interface type K,
     * provided that J is not a subinterface of K and there is no method name m
     * such that J and K both contain a method named m with the same signature
     * but different return types. From any array type SC[] to any array type
     * TC[], provided that SC and TC are reference types and there is a
     * narrowing conversion from SC to TC.
     * 
     * @link 
     *       http://java.sun.com/docs/books/jls/second_edition/html/conversions.doc
     *       .html
     * @param from
     * @param to
     * @return
     */
    public boolean allowJavaUpcast(Class<?> from, Class<?> to) {
        if (from.isAssignableFrom(to)) {
            return true;
        }

        if (!from.isPrimitive() && to.isInterface()) {
            return true;
        }

        if (!to.isPrimitive() && from.isInterface()) {
            return true;
        }

        return false;
    }

}
