package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.util.List;

import org.openl.extension.xmlrules.model.DataInstance;
import org.openl.extension.xmlrules.model.single.DataInstanceImpl;
import org.openl.extension.xmlrules.model.single.Reference;
import org.openl.extension.xmlrules.model.single.ValuesRow;

public class LazyDataInstance extends BaseLazyItem<DataInstanceImpl> implements DataInstance {
    public LazyDataInstance(File file, String entryName) {
        super(file, entryName);
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
    public List<String> getFields() {
        return getInfo().getFields();
    }

    @Override
    public List<Reference> getReferences() {
        return getInfo().getReferences();
    }

    @Override
    public List<ValuesRow> getValues() {
        return getInfo().getValues();
    }
}
