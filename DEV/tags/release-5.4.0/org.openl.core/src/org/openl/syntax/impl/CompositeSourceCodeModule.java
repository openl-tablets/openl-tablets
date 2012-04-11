/*
 * Created on Oct 13, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.openl.syntax.impl;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.openl.IOpenSourceCodeModule;

/**
 * @author snshor
 *
 */
public class CompositeSourceCodeModule implements IOpenSourceCodeModule {
    IOpenSourceCodeModule[] modules;
    int[] len;

    String source;

    public CompositeSourceCodeModule(IOpenSourceCodeModule[] modules) {
        this.modules = modules;
        len = new int[modules.length];
        StringBuffer buf = new StringBuffer(100);
        for (int i = 0; i < modules.length; i++) {
            if (modules[i] == null) {
                continue;
            }
            String code = modules[i].getCode();
            len[i] = code.length();
            buf.append(code);
        }

        source = buf.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenSourceCodeModule#getByteStream()
     */
    public InputStream getByteStream() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenSourceCodeModule#getCharacterStream()
     */
    public Reader getCharacterStream() {
        return new StringReader(source);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenSourceCodeModule#getCode()
     */
    public String getCode() {
        return source;
    }

    public int getStartPosition() {
        return 0;
    }

    public int getTabSize() {
        return 2;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenSourceCodeModule#getUri(int)
     */
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
