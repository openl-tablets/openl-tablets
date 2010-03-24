package org.openl.source.impl;

import java.io.InputStream;
import java.io.Reader;

import org.openl.source.IOpenSourceCodeModule;

public class SourceCodeModuleDelegator implements IOpenSourceCodeModule {
    protected IOpenSourceCodeModule src;

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

    public int getTabSize() {
        return src.getTabSize();
    }

    public String getUri(int textpos) {
        return src.getUri(textpos);
    }

}
