/*
 * Created on Oct 6, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.source.impl;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.openl.source.IOpenSourceCodeModule;

/**
 * @author snshor
 *
 */
public class StringSourceCodeModule implements IOpenSourceCodeModule {

    String code, uri;

    int tabSize = 2;

    /**
     *
     */
    public StringSourceCodeModule(String code, String uri) {
        this.code = code;
        this.uri = uri;
    }

    public StringSourceCodeModule(String code, String uri, int tabSize) {
        this(code, uri);
        this.tabSize = tabSize;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenSourceCodeModule#getByteStream()
     */
    public InputStream getByteStream() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenSourceCodeModule#getCharacterStream()
     */
    public Reader getCharacterStream() {
        return new StringReader(code);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenSourceCodeModule#getCode()
     */
    public String getCode() {
        return code;
    }

    public int getStartPosition() {
        return 0;
    }

    /**
     * @return
     */
    public int getTabSize() {
        return tabSize;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenSourceCodeModule#getUri(int)
     */
    public String getUri(int textpos) {
        return uri == null ? "http://www.openl.org/uri#internal_string" : uri;
    }

}
