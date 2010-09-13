/*
 * Created on Dec 2, 2004
 *
 * Developed by OpenRules, Inc. 2003,2004
 */
package org.openl.source.impl;

import java.io.BufferedReader;
import java.io.IOException;

import org.openl.exception.OpenLRuntimeException;
import org.openl.source.IOpenSourceCodeModule;

/**
 * @author snshor
 */
public abstract class ASourceCodeModule implements IOpenSourceCodeModule {

    protected String code;

    protected int tabSize = 2;

    protected String uri;

    public synchronized String getCode() {

        if (code == null) {
            StringBuffer buf = new StringBuffer(4096);
            char[] c = new char[8192];
            BufferedReader br = new BufferedReader(getCharacterStream());

            try {
                for (int len; (len = br.read(c)) > 0;) {
                    buf.append(c, 0, len);
                }
            } catch (IOException e) {
                throw new OpenLRuntimeException(e);
            }
            code = buf.toString();
        }
        return code;
    }

    public int getStartPosition() {
        return 0;
    }

    /**
     * @return Returns the tabSize.
     */
    public int getTabSize() {
        return tabSize;
    }

    public synchronized String getUri(int textpos) {

        if (uri == null) {
            uri = makeUri();
        }
        return uri;
    }

    protected abstract String makeUri();

    /**
     * @param tabSize The tabSize to set.
     */
    public void setTabSize(int tabSize) {
        this.tabSize = tabSize;
    }

}
