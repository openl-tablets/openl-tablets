/*
 * Created on Oct 16, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.ide.extension;

import java.util.Properties;

import org.openl.eclipse.util.IOpenlUtilBase;
import org.openl.eclipse.util.XxY;

/**
 *
 * @author sam
 */
public interface IOpenlExtensionManager extends IOpenlUtilBase, IOpenlExtensionConstants {

    /**
     * Returns properties for all OpenL extensions.
     */
    public Properties getAllOpenlExtensionsProperties();

    public XxY getFileEditorMapping();

    public XxY getFileOpenlMapping();

    /**
     * Returns OpenL builder extensions.
     */
    public IOpenlBuilderExtension[] getOpenlBuilderExtensions();

    /**
     * Returns OpenL language extensions.
     */
    public IOpenlLanguageExtension[] getOpenlLanguageExtensions();

}
