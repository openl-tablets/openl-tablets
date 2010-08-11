/*
 * Created on Oct 13, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.ide.extension;

import org.openl.eclipse.util.IOpenlConstants;

/**
 *
 * @author sam
 */
public interface IOpenlExtensionConstants extends IOpenlConstants {
    static public final String EXTENSION_POINT_OPENL_LANGUAGE = OPENL_PLUGIN_ID + ".languageConfiguration";

    static public final String EXTENSION_POINT_OPENL_BUILDER = OPENL_PLUGIN_ID + ".builderConfiguration";

    static public final IOpenlLanguageExtension[] NO_LANGUAGE_EXTENSIONS = {};

    static public final IOpenlBuilderExtension[] NO_BUILDER_EXTENSIONS = {};

}
