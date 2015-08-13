package org.openl.extension.xmlrules.model;

import java.util.List;

import org.openl.extension.xmlrules.model.single.FieldImpl;

public interface Type {
    String getName();

    List<FieldImpl> getFields();
}
