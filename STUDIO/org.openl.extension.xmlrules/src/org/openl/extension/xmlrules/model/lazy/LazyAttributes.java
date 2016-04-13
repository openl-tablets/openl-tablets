package org.openl.extension.xmlrules.model.lazy;

import java.io.File;

import org.openl.extension.xmlrules.model.single.Attributes;

public class LazyAttributes extends BaseLazyItem<Attributes> {
    public LazyAttributes(File file) {
        super(file, "attributes.xml");
    }
}
