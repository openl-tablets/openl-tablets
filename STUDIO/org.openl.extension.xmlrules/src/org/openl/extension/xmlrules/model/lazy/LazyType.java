package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import org.openl.extension.xmlrules.model.Field;
import org.openl.extension.xmlrules.model.Type;
import org.openl.extension.xmlrules.model.single.TypeImpl;

public class LazyType extends BaseLazyItem<TypeImpl> implements Type {
    public LazyType(XStream xstream, File file, String entryName) {
        super(xstream, file, entryName);
    }

    @Override
    public String getName() {
        return getInfo().getName();
    }

    @Override
    public List<Field> getFields() {
        return getInfo().getFields();
    }

}
