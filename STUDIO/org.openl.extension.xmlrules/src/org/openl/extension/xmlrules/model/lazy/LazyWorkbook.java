package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openl.extension.xmlrules.model.*;
import org.openl.extension.xmlrules.model.single.SheetInfo;
import org.openl.extension.xmlrules.model.single.WorkbookInfo;

public class LazyWorkbook extends BaseLazyItem<WorkbookInfo> {
    private WorkbookInfo info;

    public LazyWorkbook(File file,
            String entryName,
            WorkbookInfo info) {
        super(file, entryName);
        this.info = info;
    }

    @Override
    public WorkbookInfo getInfo() {
        return info;
    }

    public String getXlsFileName() {
        return getInfo().getXlsFileName();
    }

    public List<Sheet> getSheets() {
        List<Sheet> sheets = new ArrayList<Sheet>();
        List<SheetInfo> loadedSheets = getInfo().getSheets();
        for (int i = 0; i < loadedSheets.size(); i++) {
            loadedSheets.get(i).setId(i + 1);
        }
        for (final SheetInfo sheetInfo : loadedSheets) {
            sheets.add(new Sheet() {
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
                    //TODO Add postProcess step
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
                    //TODO Add postProcess step
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
                    //TODO Add postProcess step
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
                    //TODO Add postProcess step
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
                    //TODO Add postProcess step
                    List<LazyCells> result = new ArrayList<LazyCells>();
                    List<String> entryNames = sheetInfo.getCellEntries();
                    if (entryNames == null) {
                        return null;
                    }
                    for (String entryName : entryNames) {
                        result.add(new LazyCells(getFile(), entryName, getXlsFileName(), getName()));
                    }
                    return result;
                }
            });
        }
        return sheets;
    }
}
