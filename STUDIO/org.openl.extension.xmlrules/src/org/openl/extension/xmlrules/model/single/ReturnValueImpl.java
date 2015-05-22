package org.openl.extension.xmlrules.model.single;

import org.openl.extension.xmlrules.model.ReturnValue;

public class ReturnValueImpl implements ReturnValue {
    private String value;
    private XlsRegionImpl region;

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public XlsRegionImpl getRegion() {
        return region;
    }

    public void setRegion(XlsRegionImpl region) {
        this.region = region;
    }
}
