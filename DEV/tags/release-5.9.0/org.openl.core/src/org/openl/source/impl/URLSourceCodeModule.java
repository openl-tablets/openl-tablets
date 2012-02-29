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
import java.net.URLConnection;

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
    
    public long getLastModified() {
    	InputStream is = null;
        try {
        	URLConnection conn = url.openConnection();
        	long lastModified = conn.getLastModified();
        	
        	// FileURLConnection#getLastModified() opens an input stream to get the last modified date.
        	// It should be closed explicitly.
        	//
        	is = conn.getInputStream();
        	
            return lastModified;
        } catch (IOException e) {
            LOG.warn(String.format("Failed to open connection for URL \"%s\"", url.toString()), e);
            return -1;
        } finally {
        	if (is != null) {
        		try { 
        			is.close();
        		} catch (IOException e) {
					// ignore
				}
        	}
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

    @Override
    public void reset() {
        lastModified = getLastModified();
    }

}