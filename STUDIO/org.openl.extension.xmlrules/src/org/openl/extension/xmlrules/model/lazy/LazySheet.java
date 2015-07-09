package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import org.openl.extension.xmlrules.model.*;

public class LazySheet extends BaseLazyItem<SheetInfo> implements Sheet {
    public LazySheet(XStream xstream, File file, String entryName) {
        super(xstream, file, entryName);
    }

    @Override
    public String getName() {
        return getInfo().getName();
    }

    @Override
    public List<Type> getTypes() {
        //TODO Add postProcess step
        List<Type> result = new ArrayList<Type>();
        List<String> entryNames = getInfo().getTypeEntries();
        if (entryNames == null) {
            return null;
        }
        for (String entryName : entryNames) {
            result.add(new LazyType(getXstream(), getFile(), entryName));
        }
        return result;
    }

    @Override
    public List<DataInstance> getDataInstances() {
        //TODO Add postProcess step
        List<DataInstance> result = new ArrayList<DataInstance>();
        List<String> entryNames = getInfo().getDataInstanceEntries();
        if (entryNames == null) {
            return null;
        }
        for (String entryName : entryNames) {
            result.add(new LazyDataInstance(getXstream(), getFile(), entryName));
        }
        return result;
    }

    @Override
    public List<Table> getTables() {
        //TODO Add postProcess step
        List<Table> result = new ArrayList<Table>();
        List<String> entryNames = getInfo().getTableEntries();
        if (entryNames == null) {
            return null;
        }
        for (String entryName : entryNames) {
            result.add(new LazyTable(getXstream(), getFile(), entryName));
        }
        return result;
    }

    @Override
    public List<Function> getFunctions() {
        //TODO Add postProcess step
        List<Function> result = new ArrayList<Function>();
        List<String> entryNames = getInfo().getFunctionEntries();
        if (entryNames == null) {
            return null;
        }
        for (String entryName : entryNames) {
            result.add(new LazyFunction(getXstream(), getFile(), entryName));
        }
        return result;
    }
}
