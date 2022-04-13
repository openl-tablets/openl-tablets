package org.openl.rules.repository.aws;

import java.io.IOException;
import java.io.InputStream;

import org.openl.util.IOUtils;

/**
 * The purpose of this class, is to drain S3 input stream before closing it. It's required, because not all bytes are
 * read by {@link java.util.zip.ZipInputStream} which leads abnormal connection aborting and logs spamming
 */
class DrainableInputStream extends InputStream {

    private final InputStream delegate;

    public DrainableInputStream(InputStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public int read() throws IOException {
        return delegate.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return delegate.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return delegate.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return delegate.skip(n);
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    @SuppressWarnings("StatementWithEmptyBody")
    public void close() {
        try {
            while (delegate.read() > -1) {
                // make sure that stream was fully read
            }
        } catch (IOException ignored) {
        } finally {
            IOUtils.closeQuietly(delegate);
        }
    }

    @Override
    public void mark(int readlimit) {
        delegate.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        delegate.reset();
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }

}
