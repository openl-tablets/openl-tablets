package org.openl.extension.xmlrules.model.single;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.*;

@XmlRootElement(name="sheet")
@XmlType(name = "sheet")
public class SheetInfo {
    private Integer id;
    private String name;
    private List<String> typeEntries = new ArrayList<String>();
    private List<String> dataInstanceEntries = new ArrayList<String>();
    private List<String> tableEntries = new ArrayList<String>();
    private List<String> functionEntries = new ArrayList<String>();

    private List<String> cellEntries = new ArrayList<String>();

    @XmlTransient
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElementWrapper(name="type-entries", required = true)
    @XmlElement(name = "string")
    public List<String> getTypeEntries() {
        return typeEntries;
    }

    public void setTypeEntries(List<String> typeEntries) {
        this.typeEntries = typeEntries;
    }

    @XmlElementWrapper(name="data-instance-entries", required = true)
    @XmlElement(name = "string")
    public List<String> getDataInstanceEntries() {
        return dataInstanceEntries;
    }

    public void setDataInstanceEntries(List<String> dataInstanceEntries) {
        this.dataInstanceEntries = dataInstanceEntries;
    }

    @XmlElementWrapper(name="table-entries", required = true)
    @XmlElement(name = "string")
    public List<String> getTableEntries() {
        return tableEntries;
    }

    public void setTableEntries(List<String> tableEntries) {
        this.tableEntries = tableEntries;
    }

    @XmlElementWrapper(name="function-entries", required = true)
    @XmlElement(name = "string")
    public List<String> getFunctionEntries() {
        return functionEntries;
    }

    public void setFunctionEntries(List<String> functionEntries) {
        this.functionEntries = functionEntries;
    }

    @XmlElementWrapper(name="cell-entries", required = true)
    @XmlElement(name = "string")
    public List<String> getCellEntries() {
        return cellEntries;
    }

    public void setCellEntries(List<String> cellEntries) {
        this.cellEntries = cellEntries;
    }
}
