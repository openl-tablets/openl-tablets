package org.openl.extension.xmlrules.model;

import java.util.List;

public interface DataInstance {
    String getType();

    String getName();

    List<Field> getFields();

    List<List<String>> getValues();
}
