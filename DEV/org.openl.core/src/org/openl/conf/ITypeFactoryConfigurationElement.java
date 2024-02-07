/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import org.openl.types.ITypeLibrary;

/**
 * @author snshor
 */

public interface ITypeFactoryConfigurationElement extends IConfigurationElement {

    ITypeLibrary getLibrary(IConfigurableResourceContext cxt);

}
