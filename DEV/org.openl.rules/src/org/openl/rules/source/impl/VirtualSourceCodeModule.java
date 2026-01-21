package org.openl.rules.source.impl;

import java.io.InputStream;
import java.io.Reader;

import org.openl.source.impl.ASourceCodeModule;

@Deprecated
public class VirtualSourceCodeModule extends ASourceCodeModule {

    public static final String SOURCE_URI = "<virtual_uri>";

    @Override
    protected String makeUri() {
        return SOURCE_URI;
    }

    @Override
    public InputStream getByteStream() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Reader getCharacterStream() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
