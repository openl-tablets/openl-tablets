/*
 * Created on Oct 16, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.ide.extension;

import java.util.Properties;

import org.openl.eclipse.util.IOpenlUtilBase;

/**
 *
 * @author sam
 */
public interface IOpenlExtensionBase extends IOpenlUtilBase, IOpenlExtensionConstants {
    public String getId();

    public String getName();

    public Properties getProperties();

    // public IEPluginDescriptor getDeclaringPluginDescriptor();

}
