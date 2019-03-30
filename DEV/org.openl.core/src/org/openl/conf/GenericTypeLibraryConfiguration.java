/*
 * Created on Jun 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import org.openl.types.ITypeLibrary;

/**
 * @author snshor
 *
 */
public class GenericTypeLibraryConfiguration extends AGenericConfiguration implements ITypeFactoryConfigurationElement {

    private ITypeLibrary library;

    @Override
    public Class<?> getImplementingClass() {
        return ITypeLibrary.class;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.conf.IMethodFactoryConfigurationElement#getLibrary(org.openl.conf.IConfigurableResourceContext)
     */
    @Override
    public synchronized ITypeLibrary getLibrary(IConfigurableResourceContext cxt) {

        if (library == null) {
            library = (ITypeLibrary) createResource(cxt);
        }
        return library;

    }

}
