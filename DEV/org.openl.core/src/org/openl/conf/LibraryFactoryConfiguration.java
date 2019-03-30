/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.util.CategorizedMap;

/**
 * @author snshor
 *
 */
public class LibraryFactoryConfiguration extends AConfigurationElement implements IConfigurationElement {

    CategorizedMap map = new CategorizedMap();

    public void addConfiguredLibrary(NameSpacedLibraryConfiguration library) {
        map.put(library.getNamespace(), library);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INameSpacedMethodFactory#getMethodCaller(java.lang.String, java.lang.String,
     * org.openl.types.IOpenClass[], org.openl.binding.ICastFactory)
     */
    public IOpenMethod[] getMethods(String namespace, String name, IConfigurableResourceContext cxt) {
        NameSpacedLibraryConfiguration lib = (NameSpacedLibraryConfiguration) map.get(namespace);
        return lib == null ? new IOpenMethod[] {} : lib.getMethods(name, cxt);
    }

    public IOpenField getVar(String namespace, String name, IConfigurableResourceContext cxt, boolean strictMatch) {
        NameSpacedLibraryConfiguration lib = (NameSpacedLibraryConfiguration) map.get(namespace);
        return lib == null ? null : lib.getField(name, cxt, strictMatch);
    }

    @Override
    public void validate(IConfigurableResourceContext cxt) {
        for (Object lib : map.values()) {
            ((NameSpacedLibraryConfiguration) lib).validate(cxt);
        }
    }
}
