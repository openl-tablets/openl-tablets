/*
 * Created on May 6, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;

import org.openl.base.INameSpacedThing;
import org.openl.base.NameSpacedThing;

/**
 * @author snshor
 *
 * This class is the base of all the type definitions. It should not be treated
 * as substitute for Java Class, even though in many instances it is.
 * <p>
 * IType provides very generic functionality, but allows to provide such
 * non-java features as using non-java types, using composite types(for example
 * int, Integer, BigInteger) etc.
 */
public interface IType extends INameSpacedThing {

    class AnyThing extends NameSpacedThing implements IType {

        private AnyThing() {
            super("Any", "http://domain.openl.org");
        }

        public IDomain<Object> getDomain() {
            return null;
        }

        public boolean isAssignableFrom(IType type) {
            return true;
        }

        public boolean isInstance(Object obj) {
            return true;
        }
    }

    AnyThing ANY = new AnyThing();

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
     * Please note how it is similar to selector or domain methods
     *
     */
    boolean isInstance(Object obj);

    // TODO static public class JavaType implements IType

}
