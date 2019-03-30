/*
 * Created on Dec 2, 2004
 *
 * Developed by OpenRules, Inc. 2003,2004
 */
package org.openl.source.impl;

import org.openl.util.RuntimeExceptionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

/**
 * @author snshor
 */
public class URLSourceCodeModule extends ASourceCodeModule {
    private final Logger log = LoggerFactory.getLogger(URLSourceCodeModule.class);

    private URL url;
    private long lastModified;

    public URLSourceCodeModule(URL url) {
        this.url = url;
        lastModified = getLastModified();
    }

    public URLSourceCodeModule(String file) {
        this(toUrl(new File(file)));
    }

    public static URL toUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
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
            log.warn("Failed to open connection for URL \"{}\"", url, e);
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

    @Override
    public InputStream getByteStream() {
        try {
            return url.openStream();
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    @Override
    public Reader getCharacterStream() {
        return new InputStreamReader(getByteStream());
    }

    @Override
    protected String makeUri() {
        // FIXME spaces are not supported according to the URI specification.
        // Correct way of url-to-uri conversion should be found in order to
        // process all illegal characters
        return url.toExternalForm().replace(" ", "%20");
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof URLSourceCodeModule)) {
            return false;
        }

        URLSourceCodeModule urlSource = (URLSourceCodeModule) obj;

        return Objects.equals(url, urlSource.url);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(url);
    }

    @Override
    public String toString() {
        return url.toString();
    }

    @Override
    public boolean isModified() {
        return getLastModified() != lastModified;
    }

    @Override
    public void resetModified() {
        lastModified = getLastModified();
    }

}