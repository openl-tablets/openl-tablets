package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openl.extension.xmlrules.model.*;
import org.openl.extension.xmlrules.model.single.SheetInfo;

public class LazySheet implements Sheet {
    private final SheetInfo sheetInfo;
    private final String workbookName;
    private final File file;

    public LazySheet(SheetInfo sheetInfo, File file, String workbookName) {
        this.file = file;
        this.sheetInfo = sheetInfo;
        this.workbookName = workbookName;
    }

    @Override
    public Integer getId() {
        return sheetInfo.getId();
    }

    @Override
    public String getName() {
        return sheetInfo.getName();
    }

    @Override
    public List<Type> getTypes() {
        List<Type> result = new ArrayList<Type>();
        List<String> entryNames = sheetInfo.getTypeEntries();
        if (entryNames == null) {
            return null;
        }
        for (String entryName : entryNames) {
            result.add(new LazyType(getFile(), entryName));
        }
        return result;
    }

    @Override
    public List<DataInstance> getDataInstances() {
        List<DataInstance> result = new ArrayList<DataInstance>();
        List<String> entryNames = sheetInfo.getDataInstanceEntries();
        if (entryNames == null) {
            return null;
        }
        for (String entryName : entryNames) {
            result.add(new LazyDataInstance(getFile(), entryName));
        }
        return result;
    }

    @Override
    public List<Table> getTables() {
        List<Table> result = new ArrayList<Table>();
        List<String> entryNames = sheetInfo.getTableEntries();
        if (entryNames == null) {
            return null;
        }
        for (String entryName : entryNames) {
            result.add(new LazyTable(getFile(), entryName));
        }
        return result;
    }

    @Override
    public List<Function> getFunctions() {
        List<Function> result = new ArrayList<Function>();
        List<String> entryNames = sheetInfo.getFunctionEntries();
        if (entryNames == null) {
            return null;
        }
        for (String entryName : entryNames) {
            result.add(new LazyFunction(getFile(), entryName));
        }
        return result;
    }

    @Override
    public List<LazyCells> getCells() {
        List<LazyCells> result = new ArrayList<LazyCells>();
        List<String> entryNames = sheetInfo.getCellEntries();
        if (entryNames == null) {
            return null;
        }
        for (String entryName : entryNames) {
            result.add(new LazyCells(getFile(), entryName, workbookName, getName()));
        }
        return result;
    }

    @Override
    public String getWorkbookName() {
        return workbookName;
    }

    protected File getFile() {
        return file;
    }
}
