/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import org.openl.binding.exception.AmbiguousTypeException;
import org.openl.types.IOpenClass;
import org.openl.util.CategorizedMap;

/**
 * @author snshor
 *
 */
public class TypeFactoryConfiguration extends AConfigurationElement implements IConfigurationElement {

    private CategorizedMap map = new CategorizedMap();

    public void addConfiguredTypeLibrary(NameSpacedTypeConfiguration library) {
        map.put(library.getNamespace(), library);
    }

    public IOpenClass getType(String namespace,
            String name,
            IConfigurableResourceContext cxt) throws AmbiguousTypeException {
        NameSpacedTypeConfiguration lib = (NameSpacedTypeConfiguration) map.get(namespace);
        return lib == null ? null : lib.getType(name, cxt);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.newconf.IConfigurationElement#validate(org.openl.newconf.IConfigurationContext)
     */
    @Override
    public void validate(IConfigurableResourceContext cxt) {
        for (Object lib : map.values()) {
            ((NameSpacedTypeConfiguration) lib).validate(cxt);
        }
    }

}
