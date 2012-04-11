/*
 * Created on Oct 16, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.internal.ide.extension;

import org.eclipse.core.runtime.IConfigurationElement;
import org.openl.eclipse.ide.extension.IOpenlBuilderExtension;

/**
 *
 * @author sam
 */
public class OpenlBuilderExtension extends OpenlExtensionBase implements IOpenlBuilderExtension {

    public OpenlBuilderExtension(IConfigurationElement element) {
        super(element);
    }

}
