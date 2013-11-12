/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import org.openl.types.ITypeLibrary;
import org.openl.types.impl.ImportTypeLibrary;
import org.openl.util.CollectionsUtil;

/**
 * @author snshor
 *
 */
public class ImportTypeConfiguration extends AConfigurationElement implements ITypeFactoryConfigurationElement {

    private String[] classes = {};

    private String[] imports = {};

    private ITypeLibrary library = null;

    private OpenSchemaConfiguration config = null;

    public void addConfiguredClassName(String className) {
        classes = (String[]) CollectionsUtil.add(classes, className);
    }

    public void addConfiguredImport(String anImport) {
        imports = (String[]) CollectionsUtil.add(imports, anImport);
    }

    public void addSchema(OpenSchemaConfiguration schema) {
        config = schema;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.newconf.IMethodFactoryConfigurationElement#getFactory()
     */
    public synchronized ITypeLibrary getLibrary(IConfigurableResourceContext cxt) {
        if (library == null) {
            library = new ImportTypeLibrary(config.getLibrary(cxt), imports, classes);
        }
        return library;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.newconf.IConfigurationElement#validate(org.openl.newconf.IConfigurationContext)
     */
    public void validate(IConfigurableResourceContext cxt) throws OpenConfigurationException {
        if (config == null) {
            throw new OpenConfigurationException("Library must have schema configuration", getUri(), null);
        }
        config.validate(cxt);

    }

}
