package org.openl.extension.xmlrules.model;

import java.util.List;

import org.openl.extension.xmlrules.model.single.ConditionImpl;
import org.openl.extension.xmlrules.model.single.ParameterImpl;
import org.openl.extension.xmlrules.model.single.ReturnRow;

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
}
