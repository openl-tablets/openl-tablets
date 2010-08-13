/*
 * Created on Oct 16, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.internal.ide.extension;

import java.util.Properties;

import org.eclipse.core.runtime.IConfigurationElement;
import org.openl.eclipse.ide.extension.IOpenlExtensionBase;
import org.openl.eclipse.util.UtilBase;

/**
 *
 * @author sam
 */
public class OpenlExtensionBase extends UtilBase implements IOpenlExtensionBase {
    protected IConfigurationElement element;

    // protected IPluginDescriptor pluginDescriptor;

    protected String id;

    protected String name;

    protected Properties properties;

    public OpenlExtensionBase(IConfigurationElement element) {
        this.element = element;
        // this.pluginDescriptor = pluginDescriptor;

        init();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Properties getProperties() {
        return properties;
    }

    protected void init() {
        id = element.getAttribute("id");
        name = element.getAttribute("name");
        properties = new Properties();

        IConfigurationElement[] props = element.getChildren("property");
        for (int i = 0; i < props.length; i++) {
            String name = props[i].getAttribute("name");
            String value = props[i].getAttribute("value");

            // String resolvedValue =
            // getDeclaringPluginDescriptor().getMacros().process(value);

            properties.put(name, value);
        }

    }

    // public IPluginDescriptor getDeclaringPluginDescriptor()
    // {
    // return pluginDescriptor;
    // }

}
