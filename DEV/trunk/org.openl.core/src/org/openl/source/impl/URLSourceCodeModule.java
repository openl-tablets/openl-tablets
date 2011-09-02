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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author snshor
 */
public class URLSourceCodeModule extends ASourceCodeModule {
    private static Log LOG = LogFactory.getLog(URLSourceCodeModule.class);

    private URL url;
    private long lastModified;

    public URLSourceCodeModule(URL url) {
        this.url = url;
        lastModified = getLastModified();
    }
    
    public URL getUrl() {
        return url;
    }
    
    public long getLastModified(){
        try {
            return url.openConnection().getLastModified();
        } catch (IOException e) {
            LOG.warn(String.format("Failed to open connection for URL \"%s\"", url.toString()), e);
            return -1;
        }
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

    public boolean isModified() {
        return getLastModified() != lastModified;
    }

}