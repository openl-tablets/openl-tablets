/*
 * Created on May 6, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;

import org.openl.base.INamedThing;

/**
 * @author snshor
 * 
 *         This class is the base of all the type definitions. It should not be
 *         treated as substitute for Java Class, even though in many instances
 *         it is.
 *         <p>
 *         IType provides very generic functionality, but allows to provide such
 *         non-java features as using non-java types, using composite types(for
 *         example int, Integer, BigInteger) etc.
 */
public interface IType extends INamedThing {

    /**
     * Provides type validation(usually by constraining type)
     * 
     * @return
     */
    IDomain<?> getDomain();

    /**
     * 
     * @param type
     * @return true if a type is specialization of more general this type if
     *         (T1.isAssignableFrom(T2) AND T2.isInstance(x)) ->
     *         T1.isInstance(x)
     */
    boolean isAssignableFrom(IType type);

    /**
     * @param obj
     * @return true if the object belongs to this type
     * 
     *         Please note how it is similar to selector or domain methods
     * 
     */
    boolean isInstance(Object obj);

    // TODO static public class JavaType implements IType

}
