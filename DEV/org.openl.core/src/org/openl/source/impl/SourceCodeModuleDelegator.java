package org.openl.source.impl;

import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import org.openl.source.IOpenSourceCodeModule;

public class SourceCodeModuleDelegator implements IOpenSourceCodeModule {

    protected IOpenSourceCodeModule src;
    private Map<String, Object> params;

    public SourceCodeModuleDelegator(IOpenSourceCodeModule src) {
        this.src = src;
    }

    public InputStream getByteStream() {
        return src.getByteStream();
    }

    public Reader getCharacterStream() {
        return src.getCharacterStream();
    }

    public String getCode() {
        return src.getCode();
    }

    public int getStartPosition() {
        return src.getStartPosition();
    }

    public String getUri() {
        return src.getUri();
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public boolean isModified() {
        return src.isModified();
    }

}
