package org.openl.extension.xmlrules.model;

import java.util.List;

import org.openl.extension.xmlrules.model.single.Reference;
import org.openl.extension.xmlrules.model.single.ValuesRow;

public interface DataInstance {
    String getType();

    String getName();

    List<String> getFields();

    List<Reference> getReferences();

    List<ValuesRow> getValues();
}
