package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.openl.extension.xmlrules.model.Segment;
import org.openl.extension.xmlrules.model.Table;
import org.openl.extension.xmlrules.model.single.*;

public class LazyTable extends BaseLazyItem<TableImpl> implements Table {
    public LazyTable(File file, String entryName) {
        super(file, entryName);
    }

    @Override
    public String getName() {
        return getInstance().getName();
    }

    @Override
    public List<ParameterImpl> getParameters() {
        return getInstance().getParameters();
    }

    @Override
    public String getReturnType() {
        return getInstance().getReturnType();
    }

    @Override
    public List<ConditionImpl> getHorizontalConditions() {
        return getInstance().getHorizontalConditions();
    }

    @Override
    public List<ConditionImpl> getVerticalConditions() {
        return getInstance().getVerticalConditions();
    }

    @Override
    public List<ReturnRow> getReturnValues() {
        return getInstance().getReturnValues();
    }

    @Override
    public Segment getSegment() {
        return getInstance().getSegment();
    }

    @Override
    public TableRanges getTableRanges() {
        return getInstance().getTableRanges();
    }

    @Override
    public List<Attribute> getAttributes() {
        return getInstance().getAttributes();
    }

    @Override
    protected void postProcess(TableImpl info) {
        if (info == null) {
            return;
        }
        if (info.getHorizontalConditions() == null) {
            info.setHorizontalConditions(Collections.<ConditionImpl>emptyList());
        }

        if (info.getVerticalConditions() == null) {
            info.setVerticalConditions(Collections.<ConditionImpl>emptyList());
        }
    }

}
