package org.openl.extension.xmlrules.model.lazy;

import java.util.ArrayList;
import java.util.List;

public class SheetInfo {
    private String name;
    private List<String> typeEntries = new ArrayList<String>();
    private List<String> dataInstanceEntries = new ArrayList<String>();
    private List<String> tableEntries = new ArrayList<String>();
    private List<String> functionEntries = new ArrayList<String>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTypeEntries() {
        return typeEntries;
    }

    public void setTypeEntries(List<String> typeEntries) {
        this.typeEntries = typeEntries;
    }

    public List<String> getDataInstanceEntries() {
        return dataInstanceEntries;
    }

    public void setDataInstanceEntries(List<String> dataInstanceEntries) {
        this.dataInstanceEntries = dataInstanceEntries;
    }

    public List<String> getTableEntries() {
        return tableEntries;
    }

    public void setTableEntries(List<String> tableEntries) {
        this.tableEntries = tableEntries;
    }

    public List<String> getFunctionEntries() {
        return functionEntries;
    }

    public void setFunctionEntries(List<String> functionEntries) {
        this.functionEntries = functionEntries;
    }
}
