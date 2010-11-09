/*
 * Created on Oct 6, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.source.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

import org.openl.source.IDependencyManager;
import org.openl.source.IOpenSourceCodeModule;

/**
 * @author snshor
 * 
 */
public class StringSourceCodeModule implements IOpenSourceCodeModule {

    private String code;
    private String uri;

    private int tabSize = 2;
    private Map<String, Object> params;
    
    private IDependencyManager depManager;

    public StringSourceCodeModule(String code, String uri) {
        this.code = code;
        this.uri = uri;
    }

    public StringSourceCodeModule(String code, String uri, int tabSize) {
        this(code, uri);
        this.tabSize = tabSize;
    }

    public InputStream getByteStream() {
        return new ByteArrayInputStream(code.getBytes());
    }

    public Reader getCharacterStream() {
        return new StringReader(code);
    }

    public String getCode() {
        return code;
    }

    public int getStartPosition() {
        return 0;
    }

    public int getTabSize() {
        return tabSize;
    }

    public String getUri(int textpos) {
        return uri;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public IDependencyManager getDepManager() {
        return depManager;
    }

    public void setDepManager(IDependencyManager depManager) {
        this.depManager = depManager;
    }
    
    
    
}
