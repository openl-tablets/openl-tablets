/*
 * Created on Jun 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

/**
 * @author snshor
 *
 */
public interface IConfigurationElement {
    /**
     *
     * @return URI of this element (if known) or null
     */
    String getUri();

    // Object getResource(IConfigurationContext cxt) throws
    // OpenConfigurationException;

    /**
     * Checks that configuration is valid, for example that class exists and conforms to the interface
     *
     * @throws OpenConfigurationException
     */

    void validate(IConfigurableResourceContext cxt) throws OpenConfigurationException;
}
