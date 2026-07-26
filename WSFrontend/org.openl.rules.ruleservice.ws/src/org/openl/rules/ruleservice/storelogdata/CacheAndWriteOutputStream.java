package org.openl.rules.ruleservice.storelogdata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.cxf.io.CachedOutputStream;

/**
 * Buffers the response body instead of streaming it through.
 *
 * <p>
 * Every byte written is held back until {@link #copyCacheToFlowThroughStream()} releases it to the target stream.
 * This keeps the response from reaching the client before synchronous store-log-data has finished.
 *
 * <p>
 * {@link org.apache.cxf.io.CacheAndWriteOutputStream} writes to the target stream immediately and is used instead
 * whenever no synchronous logging is configured.
 */
class CacheAndWriteOutputStream extends CachedOutputStream {

    private final OutputStream flowThroughStream;
    private final ByteArrayOutputStream flowThroughStreamCache = new ByteArrayOutputStream();

    public CacheAndWriteOutputStream(OutputStream stream) {
        super();
        if (stream == null) {
            throw new IllegalArgumentException("Stream may not be null");
        }
        flowThroughStream = stream;
    }

    public void copyCacheToFlowThroughStream() throws IOException {
        flowThroughStreamCache.flush();
        flowThroughStream.write(flowThroughStreamCache.toByteArray());
        flowThroughStream.close();
        flowThroughStreamCache.reset();
    }

    @Override
    public void write(int b) throws IOException {
        flowThroughStreamCache.write(b);
        super.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        flowThroughStreamCache.write(b, off, len);
        super.write(b, off, len);
    }

    @Override
    public void write(byte[] b) throws IOException {
        flowThroughStreamCache.write(b);
        super.write(b);
    }
}
