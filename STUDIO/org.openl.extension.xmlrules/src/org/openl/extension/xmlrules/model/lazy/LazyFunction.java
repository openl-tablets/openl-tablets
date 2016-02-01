package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.util.List;

import org.openl.extension.xmlrules.model.Function;
import org.openl.extension.xmlrules.model.single.Attribute;
import org.openl.extension.xmlrules.model.single.FunctionImpl;
import org.openl.extension.xmlrules.model.single.ParameterImpl;

public class LazyFunction extends BaseLazyItem<FunctionImpl> implements Function {
    public LazyFunction(File file, String entryName) {
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
    public String getCellAddress() {
        return getInfo().getCellAddress();
    }

    @Override
    public List<Attribute> getAttributes() {
        return getInfo().getAttributes();
    }

}
