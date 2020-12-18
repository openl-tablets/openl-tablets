/*
 * Created on Dec 2, 2004
 *
 * Developed by OpenRules, Inc. 2003,2004
 */
package org.openl.source.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author snshor
 */
public class URLSourceCodeModule extends ASourceCodeModule {
    private final URL url;

    public URLSourceCodeModule(URL url) {
        this.url = url;
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
}