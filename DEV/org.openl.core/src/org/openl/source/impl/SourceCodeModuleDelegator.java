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

    @Override
    public InputStream getByteStream() {
        return src.getByteStream();
    }

    @Override
    public Reader getCharacterStream() {
        return src.getCharacterStream();
    }

    @Override
    public String getCode() {
        return src.getCode();
    }

    @Override
    public int getStartPosition() {
        return src.getStartPosition();
    }

    @Override
    public String getUri() {
        return src.getUri();
    }

    @Override
    public Map<String, Object> getParams() {
        return params;
    }

    @Override
    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public boolean isModified() {
        return src.isModified();
    }

}
