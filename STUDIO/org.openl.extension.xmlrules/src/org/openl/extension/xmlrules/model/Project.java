package org.openl.extension.xmlrules.model;

import java.util.List;

public interface Project {
    String getXlsFileName();

    List<Type> getTypes();

    List<DataInstance> getDataInstances();

    List<Table> getTables();

    List<Function> getFunctions();
}
