package org.openl.rules.ruleservice.resolver;

class WSEntryPoint {
    private String fullFilename;
    private boolean _interface;

    WSEntryPoint(String fullFilename, boolean _interface) {
        this.fullFilename = fullFilename;
        this._interface = _interface;
    }

    public String getFullFilename() {
        return fullFilename;
    }

    public boolean isInterface() {
        return _interface;
    }
}
