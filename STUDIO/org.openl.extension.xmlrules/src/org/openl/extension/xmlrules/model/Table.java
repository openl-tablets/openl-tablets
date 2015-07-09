package org.openl.extension.xmlrules.model;

import java.util.List;

/**
 * @author nsamatov.
 */
public interface Table {
    String getName();

    List<Parameter> getParameters();

    String getReturnType();

    List<Condition> getHorizontalConditions();

    List<Condition> getVerticalConditions();

    List<List<Expression>> getReturnValues();

    Segment getSegment();
}
