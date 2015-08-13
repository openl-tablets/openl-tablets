package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.util.List;

import org.openl.extension.xmlrules.model.Type;
import org.openl.extension.xmlrules.model.single.FieldImpl;
import org.openl.extension.xmlrules.model.single.TypeImpl;

public class LazyType extends BaseLazyItem<TypeImpl> implements Type {
    public LazyType(File file, String entryName) {
        super(file, entryName);
    }

    @Override
    public String getName() {
        return getInfo().getName();
    }

    @Override
    public List<FieldImpl> getFields() {
        return getInfo().getFields();
    }

}
