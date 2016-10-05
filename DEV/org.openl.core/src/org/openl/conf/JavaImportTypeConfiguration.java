/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.util.ArrayList;
import java.util.List;

import org.openl.types.ITypeLibrary;
import org.openl.types.java.JavaImportTypeLibrary;
import org.openl.util.StringUtils;

/**
 * @author snshor
 *
 */
public class JavaImportTypeConfiguration extends AConfigurationElement implements ITypeFactoryConfigurationElement {
    
    private List<String> classes = new ArrayList<String>();

    private List<String> packages = new ArrayList<String>();

    private ITypeLibrary library = null;

    public void addClassImport(String className) {
        if (StringUtils.isNotEmpty(className)) {
            classes.add(className);
        }
    }

    public void addPackageImport(String packageName) {
        if (StringUtils.isNotEmpty(packageName)) {
            packages.add(packageName);
        }   
    }

    public synchronized ITypeLibrary getLibrary(IConfigurableResourceContext cxt) {
        if (library == null) {
            library = new JavaImportTypeLibrary(classes, packages, cxt.getClassLoader());
        }
        return library;
    }

    public void validate(IConfigurableResourceContext cxt) throws OpenConfigurationException {
    }

}
