package org.openl.conf;

import org.openl.types.ITypeLibrary;
import org.openl.types.java.JavaLongNameTypeLibrary;

public class JavaLongNameTypeConfiguration extends AConfigurationElement implements ITypeFactoryConfigurationElement {

    public void validate(IConfigurableResourceContext cxt) {
        // TODO Auto-generated method stub

    }

    private JavaLongNameTypeLibrary library;
    
    public synchronized ITypeLibrary getLibrary(IConfigurableResourceContext cxt) {
        if (library == null) {
            library = new JavaLongNameTypeLibrary(cxt.getClassLoader());
        }
        return library;
    }

}
