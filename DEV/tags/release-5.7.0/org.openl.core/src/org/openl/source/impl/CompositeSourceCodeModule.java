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

import org.openl.source.IOpenSourceCodeModule;

/**
 * @author snshor
 *
 */
public class CompositeSourceCodeModule implements IOpenSourceCodeModule {
    IOpenSourceCodeModule[] modules;
    int[] len;

    private String source;

    public CompositeSourceCodeModule(IOpenSourceCodeModule[] modules, String separator) {
        this.modules = modules;
        len = new int[modules.length];
        makeCode(separator);
    }
    
    
    private void makeCode(String separator)
    {
        StringBuffer buf = new StringBuffer(100);
        for (int i = 0; i < modules.length; i++) {
            if (modules[i] == null) {
                continue;
            }
            String code = modules[i].getCode();
            len[i] = code.length() + separator.length();
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
        for (int i = 0; i < len.length; i++) {
            if (sum + len[i] > textpos) {
                pos = i;
                relPos = textpos - sum;
                break;
            }
            sum += len[i];
        }

        return pos < 0 ? null : modules[pos].getUri(relPos);
    }

}
