package org.openl.xls.sequential;

import org.openl.IOpenParser;

public class OpenLBuilder extends org.openl.xls.OpenLBuilder {
    @Override
    protected IOpenParser createParser() {
        return new SequentialParser(getUserEnvironmentContext());
    }
}
