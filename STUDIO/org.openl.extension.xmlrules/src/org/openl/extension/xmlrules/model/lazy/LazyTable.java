package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.openl.extension.xmlrules.model.Segment;
import org.openl.extension.xmlrules.model.Table;
import org.openl.extension.xmlrules.model.single.ConditionImpl;
import org.openl.extension.xmlrules.model.single.ParameterImpl;
import org.openl.extension.xmlrules.model.single.ReturnRow;
import org.openl.extension.xmlrules.model.single.TableImpl;

public class LazyTable extends BaseLazyItem<TableImpl> implements Table {
    public LazyTable(File file, String entryName) {
        super(file, entryName);
    }

    @Override
    public String getName() {
        return getInfo().getName();
    }

    @Override
    public List<ParameterImpl> getParameters() {
        return getInfo().getParameters();
    }

    @Override
    public String getReturnType() {
        return getInfo().getReturnType();
    }

    @Override
    public List<ConditionImpl> getHorizontalConditions() {
        return getInfo().getHorizontalConditions();
    }

    @Override
    public List<ConditionImpl> getVerticalConditions() {
        return getInfo().getVerticalConditions();
    }

    @Override
    public List<ReturnRow> getReturnValues() {
        return getInfo().getReturnValues();
    }

    @Override
    public Segment getSegment() {
        return getInfo().getSegment();
    }

    @Override
    protected void postProcess(TableImpl info) {
        if (info.getHorizontalConditions() == null) {
            info.setHorizontalConditions(Collections.<ConditionImpl>emptyList());
        }

        if (info.getVerticalConditions() == null) {
            info.setVerticalConditions(Collections.<ConditionImpl>emptyList());
        }
    }

}
