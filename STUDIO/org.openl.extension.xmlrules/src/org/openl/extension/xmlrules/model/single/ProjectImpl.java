package org.openl.extension.xmlrules.model.single;

import java.util.ArrayList;
import java.util.List;

import org.openl.extension.xmlrules.model.DataInstance;
import org.openl.extension.xmlrules.model.Project;
import org.openl.extension.xmlrules.model.Table;
import org.openl.extension.xmlrules.model.Type;

public class ProjectImpl implements Project {
    private String xlsFileName;
    private List<Type> types = new ArrayList<Type>();
    private List<DataInstance> dataInstances = new ArrayList<DataInstance>();
    private List<Table> tables = new ArrayList<Table>();

    @Override
    public String getXlsFileName() {
        return xlsFileName;
    }

    public void setXlsFileName(String xlsFileName) {
        this.xlsFileName = xlsFileName;
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
}
