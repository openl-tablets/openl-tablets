/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import org.openl.OpenL;

/**
 * @author snshor
 * 
 */
public interface IOpenLBuilder {

    public OpenL build(String category) throws OpenConfigurationException;

    public void setContexts(IConfigurableResourceContext resourceContext,
            IUserContext userEnvironmentContext);

}
