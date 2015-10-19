package org.openl.extension.xmlrules.model.single;

import java.util.ArrayList;
import java.util.List;

import org.openl.extension.xmlrules.model.*;
import org.openl.extension.xmlrules.model.lazy.LazyCells;

public class SheetImpl implements Sheet, SheetHolder {
    private Integer id;
    private String name;
    private List<Type> types = new ArrayList<Type>();
    private List<DataInstance> dataInstances = new ArrayList<DataInstance>();
    private List<Table> tables = new ArrayList<Table>();
    private List<Function> functions = new ArrayList<Function>();
    private List<LazyCells> cells = new ArrayList<LazyCells>();

    private String workbookName;
    private Sheet internalSheet;

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<Type> getTypes() {
        return types;
    }

    public void setTypes(List<Type> types) {
        this.types = types;
    }

    @Override
    public List<DataInstance> getDataInstances() {
        return dataInstances;
    }

    public void setDataInstances(List<DataInstance> dataInstances) {
        this.dataInstances = dataInstances;
    }

    @Override
    public List<Table> getTables() {
        return tables;
    }

    public void setTables(List<Table> tables) {
        this.tables = tables;
    }

    @Override
    public List<Function> getFunctions() {
        return functions;
    }

    public void setFunctions(List<Function> functions) {
        this.functions = functions;
    }

    @Override
    public List<LazyCells> getCells() {
        return cells;
    }

    @Override
    public String getWorkbookName() {
        return workbookName;
    }

    public void setWorkbookName(String workbookName) {
        this.workbookName = workbookName;
    }

    public void setCells(List<LazyCells> cells) {
        this.cells = cells;
    }

    @Override
    public Sheet getInternalSheet() {
        return internalSheet;
    }

    public void setInternalSheet(Sheet internalSheet) {
        this.internalSheet = internalSheet;
    }
}
