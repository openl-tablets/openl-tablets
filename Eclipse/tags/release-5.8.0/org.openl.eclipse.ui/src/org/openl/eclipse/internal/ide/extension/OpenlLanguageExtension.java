/*
 * Created on Oct 8, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.internal.ide.extension;

import org.eclipse.core.runtime.IConfigurationElement;
import org.openl.eclipse.ide.extension.IOpenlLanguageExtension;
import org.openl.eclipse.util.XxY;
import org.openl.util.StringTool;

/**
 *
 * @author sam
 */
public class OpenlLanguageExtension extends OpenlExtensionBase implements IOpenlLanguageExtension {
    public OpenlLanguageExtension(IConfigurationElement element) {
        super(element);
    }

    public XxY getFileEditorMapping() {
        XxY m = new XxY();

        String[] extensions = getListProperty(getId(), KEY_FILE_EXTENSIONS);
        String[] editors = getListProperty(getId(), KEY_ECLIPSE_EDITORS);

        for (int i = 0; i < extensions.length; i++) {
            for (int j = 0; j < editors.length; j++) {
                m.add(extensions[i], editors[j]);
            }
        }

        return m;
    }

    public XxY getFileOpenlMapping() {
        XxY m = new XxY();

        String[] extensions = getListProperty(getId(), KEY_FILE_EXTENSIONS);

        for (int i = 0; i < extensions.length; i++) {
            m.add(extensions[i], getId());
        }

        return m;
    }

    String[] getListProperty(String baseKey, String extKey) {
        String key = baseKey + '.' + extKey;
        String value = getProperties().getProperty(key, "");
        return StringTool.tokenize(value, " \t\n\r,");
    }

}