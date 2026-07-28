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

import lombok.RequiredArgsConstructor;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.fast.FastStringReader;

/**
 * @author snshor
 */
@RequiredArgsConstructor
public class StringSourceCodeModule implements IOpenSourceCodeModule {

    private final String code;
    private final String uri;

    private Map<String, Object> params;

    @Override
    public InputStream getByteStream() {
        return new ByteArrayInputStream(code.getBytes());
    }

    @Override
    public Reader getCharacterStream() {
        return new FastStringReader(code);
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public int getStartPosition() {
        return 0;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public Map<String, Object> getParams() {
        return params;
    }

    @Override
    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
