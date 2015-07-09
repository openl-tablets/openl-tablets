package org.openl.extension.xmlrules.model;

import java.util.List;

/**
 * Analogue of Excel's sheet.
 */
public interface Sheet {
    String getName();

    List<Type> getTypes();

    List<DataInstance> getDataInstances();

    List<Table> getTables();

    List<Function> getFunctions();
}
