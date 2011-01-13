/*
 * Created on Dec 2, 2004
 *
 * Developed by OpenRules, Inc. 2003,2004
 */
package org.openl.source.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author snshor
 */
public class URLSourceCodeModule extends ASourceCodeModule {

    private URL url;

    public URLSourceCodeModule(URL url) {
        this.url = url;
    }
    
    public URL getUrl() {
        return url;
    }

    public InputStream getByteStream() {
        try {
            return url.openStream();
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    public Reader getCharacterStream() {
        return new InputStreamReader(getByteStream());
    }

    @Override
    protected String makeUri() {
        return url.toExternalForm();
    }
    
    @Override    
    public boolean equals(Object obj) {
        
        if (!(obj instanceof URLSourceCodeModule)) {
            return false;
        }
        
        URLSourceCodeModule urlSource = (URLSourceCodeModule) obj;

        return new EqualsBuilder()
            .append(url, urlSource.url)            
            .isEquals();
    }

    @Override
    public int hashCode() {
        int hashCode = new HashCodeBuilder()
            .append(url)            
            .toHashCode();
        
        return hashCode;
    }
    
    @Override
    public String toString() {
        return url.toString();
    }

}