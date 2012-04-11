/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import org.openl.OpenConfigurationException;
import org.openl.types.ITypeLibrary;
import org.openl.types.java.JavaImportTypeLibrary;
import org.openl.util.CollectionsUtil;
import org.openl.util.StringTool;

/**
 * @author snshor
 *
 */
public class JavaImportTypeConfiguration extends AConfigurationElement implements ITypeFactoryConfigurationElement {

    public static class StringHolder {
        String value;

        public void addText(String x) {
            value = x;
        }
    }
    String[] classes = {};

    String[] imports = {};

    ITypeLibrary library = null;

    public void addConfiguredClassName(StringHolder className) {
        classes = (String[]) CollectionsUtil.add(classes, className.value);
    }

    public void addConfiguredImport(StringHolder anImport) {
        imports = (String[]) CollectionsUtil.add(imports, anImport.value);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.newconf.IMethodFactoryConfigurationElement#getFactory()
     */
    public synchronized ITypeLibrary getLibrary(IConfigurableResourceContext cxt) {
        if (library == null) {
            library = new JavaImportTypeLibrary(classes, imports, cxt.getClassLoader());
        }
        return library;
    }

    public void setAll(String all) {
        imports = StringTool.tokenize(all, ";:");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.newconf.IConfigurationElement#validate(org.openl.newconf.IConfigurationContext)
     */
    public void validate(IConfigurableResourceContext cxt) throws OpenConfigurationException {
    }

}
