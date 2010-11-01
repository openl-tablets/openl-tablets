/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import org.openl.binding.ICastFactory;
import org.openl.binding.impl.StaticClassLibrary;
import org.openl.binding.impl.cast.ACastFactory;
import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.CollectionsUtil;

/**
 * @author snshor
 *
 */
public class TypeCastFactory extends AConfigurationElement implements IConfigurationElement {

    public static class JavaCastComponent extends AConfigurationElement {
        String libraryClassName;
        String className;

        ACastFactory factory = null;

        public synchronized ICastFactory getCastFactory(IConfigurableResourceContext cxt) {
            if (factory == null) {
                Class<?> libClass = ClassFactory.validateClassExistsAndPublic(libraryClassName, cxt.getClassLoader(),
                        getUri());
                Class<?> implClass = ClassFactory.validateClassExistsAndPublic(className, cxt.getClassLoader(),
                        getUri());

                factory = (ACastFactory) ClassFactory.newInstance(implClass, getUri());
                factory.setMethodFactory(new StaticClassLibrary(JavaOpenClass.getOpenClass(libClass)));
            }
            return factory;
        }

        /**
         * @param string
         */
        public void setClassName(String string) {
            className = string;
        }

        /**
         * @param string
         */
        public void setLibraryClassName(String string) {
            libraryClassName = string;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.newconf.IConfigurationElement#validate(org.openl.newconf.IConfigurationContext)
         */
        public void validate(IConfigurableResourceContext cxt) throws OpenConfigurationException {
            ClassFactory.validateClassExistsAndPublic(libraryClassName, cxt.getClassLoader(), getUri());
            Class<?> implClass = ClassFactory.validateClassExistsAndPublic(className, cxt.getClassLoader(), getUri());

            ClassFactory.validateSuper(implClass, ACastFactory.class, getUri());

            ClassFactory.validateHaveNewInstance(implClass, getUri());
        }

    }

    JavaCastComponent[] components = {};

    public void addJavaCast(JavaCastComponent cmp) {
        components = (JavaCastComponent[]) CollectionsUtil.add(components, cmp);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.ICastFactory#getCast(org.openl.types.IOpenClass,
     *      org.openl.types.IOpenClass)
     */
    public IOpenCast getCast(IOpenClass from, IOpenClass to, IConfigurableResourceContext cxt) {
        for (int i = 0; i < components.length; i++) {
            IOpenCast openCast = components[i].getCastFactory(cxt).getCast(from, to);
            if (openCast != null) {
                return openCast;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.newconf.IConfigurationElement#validate(org.openl.newconf.IConfigurationContext)
     */
    public void validate(IConfigurableResourceContext cxt) throws OpenConfigurationException {
        for (int i = 0; i < components.length; i++) {
            components[i].validate(cxt);
        }
    }

}
