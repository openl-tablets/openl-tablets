/*
 * Created on Oct 13, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.openl.source.impl;

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
public class CompositeSourceCodeModule implements IOpenSourceCodeModule {

    private IOpenSourceCodeModule[] modules;
    private String source;
    
    private int[] modulesCount;
    private Map<String, Object> params;
    
    private IDependencyManager depManager;

    public CompositeSourceCodeModule(IOpenSourceCodeModule[] modules, String separator) {
        this.modules = modules;
        this.modulesCount = new int[modules.length];

        makeCode(separator);
    }

    private void makeCode(String separator) {
        StringBuffer buf = new StringBuffer(100);
    
        for (int i = 0; i < modules.length; i++) {
            if (modules[i] == null) {
                continue;
            }
    
            String code = modules[i].getCode();
            modulesCount[i] = code.length() + separator.length();
            buf.append(code);
            buf.append(separator);
        }

        source = buf.toString();
    }

    public InputStream getByteStream() {
        throw new UnsupportedOperationException();
    }

    public Reader getCharacterStream() {
        return new StringReader(source);
    }

    public String getCode() {
        return source;
    }

    public int getStartPosition() {
        return 0;
    }

    public int getTabSize() {
        return 2;
    }

    public String getUri(int textpos) {
        int relPos = textpos;
        int sum = 0;
        int pos = -1;

        for (int i = 0; i < modulesCount.length; i++) {
            if (sum + modulesCount[i] > textpos) {
                pos = i;
                relPos = textpos - sum;
                break;
            }
            sum += modulesCount[i];
        }

        return pos < 0 ? null : modules[pos].getUri(relPos);
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