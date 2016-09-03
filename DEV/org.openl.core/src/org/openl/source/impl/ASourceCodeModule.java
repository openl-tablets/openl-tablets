/*
 * Created on Dec 2, 2004
 *
 * Developed by OpenRules, Inc. 2003,2004
 */
package org.openl.source.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author snshor
 */
public abstract class ASourceCodeModule implements IOpenSourceCodeModule {

    protected String code;
    protected String uri;

    private Map<String, Object> params;

    public synchronized String getCode() {

        if (code == null) {
            StringBuilder buf = new StringBuilder(4096);
            char[] c = new char[8192];
            BufferedReader br = new BufferedReader(getCharacterStream());

            try {
                for (int len; (len = br.read(c)) > 0;) {
                    buf.append(c, 0, len);
                }
            } catch (IOException e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }
            code = buf.toString();
        }
        return code;
    }

    public int getStartPosition() {
        return 0;
    }

    public synchronized String getUri(int textpos) {

        if (uri == null) {
            uri = makeUri();
        }
        return uri;
    }

    protected abstract String makeUri();

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public abstract void resetModified();

}
