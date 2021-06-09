/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.util.Objects;

import org.openl.binding.IOpenLibrary;
import org.openl.binding.impl.StaticClassLibrary;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public class JavaLibraryConfiguration extends AConfigurationElement implements IMethodFactoryConfigurationElement {

    private final String className;

    private volatile StaticClassLibrary library;

    public JavaLibraryConfiguration(String className) {
        this.className = Objects.requireNonNull(className, "className cannot be null");
    }

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
    public IOpenLibrary getLibrary(IConfigurableResourceContext cxt) {
        if (library == null) {
            synchronized (this) {
                if (library == null) {
                    Class<?> c = ClassFactory.validateClassExistsAndPublic(className, cxt.getClassLoader(), getUri());
                    library = new StaticClassLibrary(JavaOpenClass.getOpenClass(c));
                }
            }
        }
        return library;
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
