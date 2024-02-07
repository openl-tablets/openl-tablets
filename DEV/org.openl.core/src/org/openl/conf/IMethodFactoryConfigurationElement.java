/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import org.openl.binding.IOpenLibrary;

/**
 * @author snshor
 */
public interface IMethodFactoryConfigurationElement extends IConfigurationElement {

    IOpenLibrary getLibrary(IConfigurableResourceContext cxt);

}
