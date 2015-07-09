package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import org.openl.extension.xmlrules.model.Function;
import org.openl.extension.xmlrules.model.FunctionExpression;
import org.openl.extension.xmlrules.model.Parameter;
import org.openl.extension.xmlrules.model.single.FunctionImpl;

public class LazyFunction extends BaseLazyItem<FunctionImpl> implements Function {
    public LazyFunction(XStream xstream, File file, String entryName) {
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
    public List<FunctionExpression> getExpressions() {
        return getInfo().getExpressions();
    }

}
