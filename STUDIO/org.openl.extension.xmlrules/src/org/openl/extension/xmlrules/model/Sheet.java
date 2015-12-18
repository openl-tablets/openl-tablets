package org.openl.extension.xmlrules.model;

import java.util.List;

import org.openl.extension.xmlrules.model.lazy.LazyCells;

/**
 * Analogue of Excel's sheet.
 */
public interface Sheet {
    Integer getId();
    String getName();

    List<Type> getTypes();

    List<DataInstance> getDataInstances();

    List<Table> getTables();

    List<Function> getFunctions();

    List<LazyCells> getCells();

    String getWorkbookName();
}
