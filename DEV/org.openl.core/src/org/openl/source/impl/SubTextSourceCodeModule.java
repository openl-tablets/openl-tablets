/*
 * Created on Jan 13, 2004
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.source.impl;

import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.fast.FastStringReader;

/**
 * @author snshor
 *
 * Contains substring of the base code between start and end positions. End
 * position can have either positive or negative values. If positive, it is the
 * absolute end position from the beginning of the base code, if negative it is
 * the relative position from the end of the base code
 *
 */
public class SubTextSourceCodeModule implements IOpenSourceCodeModule {
    
    private IOpenSourceCodeModule baseModule;
    private int startPosition;
    private int endPosition = 0;

    private Map<String, Object> params;

    public SubTextSourceCodeModule(IOpenSourceCodeModule baseModule, int startPosition) {
        this.baseModule = baseModule;
        this.startPosition = startPosition;
    }

    public SubTextSourceCodeModule(IOpenSourceCodeModule baseModule, int startPosition, int endPosition) {
        this.baseModule = baseModule;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    public InputStream getByteStream() {
        return null;
    }

    public Reader getCharacterStream() {
        return new FastStringReader(getCode());
    }

    public String getCode() {
        String code = baseModule.getCode();

        int end = endPosition <= 0 ? code.length() + endPosition : endPosition;
        return code.substring(startPosition, end);
    }

    public int getStartPosition() {
        return startPosition;
    }

    public IOpenSourceCodeModule getBaseModule() {
        return baseModule;
    }

    public String getUri(int textpos) {
        return baseModule.getUri(textpos + startPosition);
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public boolean isModified() {
        return baseModule.isModified();
    }

}
