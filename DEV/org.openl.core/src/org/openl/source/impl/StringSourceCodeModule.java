/*
 * Created on Oct 6, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.source.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.fast.FastStringReader;

/**
 * @author snshor
 * 
 */
public class StringSourceCodeModule implements IOpenSourceCodeModule {

    private String code;
    private String uri;

    private Map<String, Object> params;

    public StringSourceCodeModule(String code, String uri) {
        this.code = code;
        this.uri = uri;
    }

    public InputStream getByteStream() {
        return new ByteArrayInputStream(code.getBytes());
    }

    public Reader getCharacterStream() {
        return new FastStringReader(code);
    }

    public String getCode() {
        return code;
    }

    public int getStartPosition() {
        return 0;
    }

    public String getUri() {
        return uri;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public boolean isModified() {
        return false;
    }
}
