/*
 * Created on Oct 7, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.ide.extension;

import org.openl.eclipse.util.XxY;

/**
 *
 * @author sam
 */
public interface IOpenlLanguageExtension extends IOpenlExtensionBase {

    static public final String KEY_ECLIPSE_EDITORS = "eclipse.editors";

    static public final String KEY_FILE_EXTENSIONS = "file.extensions";

    /**
     * Returns mapping: <file-extension>x<editor-id>
     */
    public XxY getFileEditorMapping();

    /**
     * Returns mapping: <file-extension>X<openl>
     */
    public XxY getFileOpenlMapping();

}
