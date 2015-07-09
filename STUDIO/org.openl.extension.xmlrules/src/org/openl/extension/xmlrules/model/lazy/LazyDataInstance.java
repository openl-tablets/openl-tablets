package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import org.openl.extension.xmlrules.model.DataInstance;
import org.openl.extension.xmlrules.model.Field;
import org.openl.extension.xmlrules.model.single.DataInstanceImpl;

public class LazyDataInstance extends BaseLazyItem<DataInstanceImpl> implements DataInstance {
    public LazyDataInstance(XStream xstream, File file, String entryName) {
        super(xstream, file, entryName);
    }

    @Override
    public String getType() {
        return getInfo().getType();
    }

    @Override
    public String getName() {
        return getInfo().getName();
    }

    @Override
    public List<Field> getFields() {
        return getInfo().getFields();
    }

    @Override
    public List<List<String>> getValues() {
        return getInfo().getValues();
    }
}
