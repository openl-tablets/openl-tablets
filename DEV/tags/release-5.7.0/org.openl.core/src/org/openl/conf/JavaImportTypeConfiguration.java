/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openl.types.ITypeLibrary;
import org.openl.types.java.JavaImportTypeLibrary;

/**
 * @author snshor
 *
 */
public class JavaImportTypeConfiguration extends AConfigurationElement implements ITypeFactoryConfigurationElement {
    
    private List<String> classes = new ArrayList<String>();

    private List<String> imports = new ArrayList<String>();

    private ITypeLibrary library = null;

    public void addConfiguredClassName(String className) {
        if (StringUtils.isNotEmpty(className)) {
            classes.add(className);
        }
    }

    public void addConfiguredImport(String anImport) {
        if (StringUtils.isNotEmpty(anImport)) {
            imports.add(anImport);
        }   
    }

    public synchronized ITypeLibrary getLibrary(IConfigurableResourceContext cxt) {
        if (library == null) {
            library = new JavaImportTypeLibrary(classes, imports, cxt.getClassLoader());
        }
        return library;
    }

    public void setAllImports(List<String> allImports) {
        if (allImports != null && !allImports.isEmpty()) {
            imports = new ArrayList<String>(allImports);
        }
    }
    
    public void setImport(String singleImport) {
        if (StringUtils.isNotEmpty(singleImport)) {
            imports.add(singleImport);
        }
    }

    public void validate(IConfigurableResourceContext cxt) throws OpenConfigurationException {
    }

}
