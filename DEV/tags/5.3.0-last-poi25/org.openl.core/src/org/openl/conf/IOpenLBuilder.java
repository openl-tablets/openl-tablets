/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import org.openl.OpenL;
import org.openl.OpenConfigurationException;

/**
 * @author snshor
 *
 */
public interface IOpenLBuilder {

    public OpenL build(String category) throws OpenConfigurationException;

    //

    public void setConfigurableResourceContext(IConfigurableResourceContext cxt, IUserContext ucxt);

}
