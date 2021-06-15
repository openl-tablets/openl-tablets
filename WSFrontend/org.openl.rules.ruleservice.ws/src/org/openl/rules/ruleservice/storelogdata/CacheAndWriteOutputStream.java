package org.openl.rules.ruleservice.storelogdata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.cxf.io.CachedOutputStream;

public class CacheAndWriteOutputStream extends CachedOutputStream {

    OutputStream flowThroughStream;
    ByteArrayOutputStream flowThroughStreamCache = new ByteArrayOutputStream();
    long count;
    long limit = Long.MAX_VALUE;

    public CacheAndWriteOutputStream(OutputStream stream) {
        super();
        if (stream == null) {
            throw new IllegalArgumentException("Stream may not be null");
        }
        flowThroughStream = stream;
    }

    public void setCacheLimit(long l) {
        limit = l;
    }

    public void closeFlowthroughStream() throws IOException {
        flowThroughStream.flush();
        flowThroughStream.close();
    }

    protected void postClose() throws IOException {
        flowThroughStream.flush();
        flowThroughStream.close();
    }

    public OutputStream getFlowThroughStream() {
        return flowThroughStream;
    }

    @Override
    protected void onWrite() throws IOException {
        // does nothing
    }

    public void copyCacheToFlowThroughStream() throws IOException {
        flowThroughStreamCache.flush();
        flowThroughStream.write(flowThroughStreamCache.toByteArray());
        flowThroughStreamCache.reset();
    }

    @Override
    public void write(int b) throws IOException {
        flowThroughStreamCache.write(b);
        if (count <= limit) {
            super.write(b);
        }
        count++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        flowThroughStreamCache.write(b, off, len);
        if (count <= limit) {
            super.write(b, off, len);
        }
        count += len;
    }

    @Override
    public void write(byte[] b) throws IOException {
        flowThroughStreamCache.write(b);
        if (count <= limit) {
            super.write(b);
        }
        count += b.length;
    }
}
