package org.openl.extension.xmlrules.model;

import java.util.List;

import org.openl.extension.xmlrules.model.single.*;

/**
 * @author nsamatov.
 */
public interface Table {
    String getName();

    List<ParameterImpl> getParameters();

    String getReturnType();

    List<ConditionImpl> getHorizontalConditions();

    List<ConditionImpl> getVerticalConditions();

    List<ReturnRow> getReturnValues();

    Segment getSegment();

    TableRanges getTableRanges();

    List<Attribute> getAttributes();
}
