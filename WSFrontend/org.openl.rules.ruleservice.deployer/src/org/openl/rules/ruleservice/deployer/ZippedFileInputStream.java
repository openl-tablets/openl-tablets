package org.openl.rules.ruleservice.deployer;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

class ZippedFileInputStream extends InputStream {

    private final ZipInputStream source;
    private boolean isClosed = false;

    public ZippedFileInputStream(ZipInputStream source) {
        this.source = source;
    }

    @Override
    public int read() throws IOException {
        return source.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return source.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return source.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return source.skip(n);
    }

    @Override
    public int available() throws IOException {
        return source.available();
    }

    @Override
    public void close() throws IOException {
        if (isClosed) {
            return; // the entry has been already closed
        }
        source.closeEntry();
        isClosed = true;
    }
}