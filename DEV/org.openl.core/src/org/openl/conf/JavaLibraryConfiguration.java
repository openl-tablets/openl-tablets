/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import org.openl.binding.IOpenLibrary;
import org.openl.binding.impl.StaticClassLibrary;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public class JavaLibraryConfiguration extends AConfigurationElement implements IMethodFactoryConfigurationElement {

    String className;

    StaticClassLibrary library = null;

    /**
     * @return
     */
    public String getClassName() {
        return className;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.newconf.IMethodFactoryConfigurationElement#getFactory()
     */
    @Override
    public synchronized IOpenLibrary getLibrary(IConfigurableResourceContext cxt) {
        if (library == null) {
            library = new StaticClassLibrary();
            Class<?> c = ClassFactory.validateClassExistsAndPublic(className, cxt.getClassLoader(), getUri());
            library.setOpenClass(JavaOpenClass.getOpenClass(c));
        }
        return library;
    }

    /**
     * @param string
     */
    public void setClassName(String string) {
        className = string;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.newconf.IConfigurationElement#validate(org.openl.newconf.IConfigurationContext)
     */
    @Override
    public void validate(IConfigurableResourceContext cxt) {
        ClassFactory.validateClassExistsAndPublic(className, cxt.getClassLoader(), getUri());
    }

}
