/*
 * Created on Jul 8, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.io.File;
import java.net.URL;

import org.openl.exception.OpenLRuntimeException;
import org.openl.types.IOpenFactory;
import org.openl.types.IOpenSchema;
import org.openl.types.ITypeLibrary;

/**
 * @author snshor
 *
 */
public class OpenSchemaConfiguration extends AConfigurationElement implements ITypeFactoryConfigurationElement {

    URL url;

    String factoryName;

    IOpenSchema schema;

    /**
     *
     */
    public OpenSchemaConfiguration() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.conf.ITypeFactoryConfigurationElement#getLibrary(org.openl.conf.IConfigurableResourceContext)
     */
    public ITypeLibrary getLibrary(IConfigurableResourceContext cxt) {
        if (schema == null) {
            try {
                IOpenFactory factory = cxt.getConfiguration().getOpenFactory(factoryName);
                schema = factory.getSchema(url.toExternalForm(), false);
            } catch (Exception ex) {
                new OpenLRuntimeException(ex);
            }
        }
        return schema;
    }

    /**
     * @param string
     */
    public void setFactory(String string) {
        factoryName = string;
    }

    public void setFile(File f) throws Exception {
        url = f.toURL();
    }

    public void setURL(String x) throws Exception {
        url = new URL(x);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.conf.IConfigurationElement#validate(org.openl.conf.IConfigurableResourceContext)
     */
    public void validate(IConfigurableResourceContext cxt) throws OpenConfigurationException {
        if (factoryName == null) {
            throw new OpenConfigurationException("Attribute factory must be set for schema", getUri(), null);
        }

        if (url == null) {
            throw new OpenConfigurationException("Either attribute <file> or <url> must be set for schema", getUri(),
                    null);
        }

        if (cxt.getConfiguration().getOpenFactory(factoryName) == null) {
            throw new OpenConfigurationException("Factory " + factoryName + " does not exist", getUri(), null);
        }

    }

}
