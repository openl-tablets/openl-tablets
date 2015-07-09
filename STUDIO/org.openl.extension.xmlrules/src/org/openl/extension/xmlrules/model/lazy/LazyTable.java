package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.util.Collections;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import org.openl.extension.xmlrules.model.*;
import org.openl.extension.xmlrules.model.single.TableImpl;

public class LazyTable extends BaseLazyItem<TableImpl> implements Table {
    public LazyTable(XStream xstream, File file, String entryName) {
        super(xstream, file, entryName);
    }

    @Override
    public String getName() {
        return getInfo().getName();
    }

    @Override
    public List<Parameter> getParameters() {
        return getInfo().getParameters();
    }

    @Override
    public String getReturnType() {
        return getInfo().getReturnType();
    }

    @Override
    public List<Condition> getHorizontalConditions() {
        return getInfo().getHorizontalConditions();
    }

    @Override
    public List<Condition> getVerticalConditions() {
        return getInfo().getVerticalConditions();
    }

    @Override
    public List<List<Expression>> getReturnValues() {
        return getInfo().getReturnValues();
    }

    @Override
    public Segment getSegment() {
        return getInfo().getSegment();
    }

    @Override
    protected void postProcess(TableImpl info) {
        if (info.getHorizontalConditions() == null) {
            info.setHorizontalConditions(Collections.<Condition>emptyList());
        }

        if (info.getVerticalConditions() == null) {
            info.setVerticalConditions(Collections.<Condition>emptyList());
        }
    }

}
