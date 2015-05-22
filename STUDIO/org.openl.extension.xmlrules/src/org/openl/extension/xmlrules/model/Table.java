package org.openl.extension.xmlrules.model;

import java.util.List;

/**
 * @author nsamatov.
 */
public interface Table {
    String getName();

    List<String> getParameters();

    List<Condition> getHorizontalConditions();

    List<Condition> getVerticalConditions();

    XlsRegion getRegion();

    List<List<ReturnValue>> getReturnValues();
}
