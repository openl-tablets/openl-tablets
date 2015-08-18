package org.openl.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A set of utils to work with general IO streams.
 *
 * @author Yury Molchan
 */
public class IOUtils {

    private static final int DEFAULT_BUFFER_SIZE = 8 * 1024;

    /**
     * Unconditionally close a <code>Closeable</code>.
     * <p/>
     * Equivalent to {@link Closeable#close()}, except any exceptions will be ignored.
     *
     * @param closeable the object to close, may be null or already closed
     */
    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    /**
     * Copy bytes from <code>InputStream</code> to an <code>OutputStream</code> and close them after.
     * <p/>
     * This method uses the provided buffer, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p/>
     * The buffer size is given by {@link #DEFAULT_BUFFER_SIZE}.
     *
     * @param input  the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     */
    public static void copyAndClose(InputStream input, OutputStream output) throws IOException {
        try {
            copy(input, output);
        } finally {
            closeQuietly(input);
            closeQuietly(output);
        }
    }

    /**
     * Copy bytes from <code>InputStream</code> to an <code>OutputStream</code>.
     * <p/>
     * This method uses the provided buffer, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p/>
     * The buffer size is given by {@link #DEFAULT_BUFFER_SIZE}.
     *
     * @param input  the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     */
    public static void copy(InputStream input, OutputStream output) throws IOException {
        copy(input, output, new byte[DEFAULT_BUFFER_SIZE]);
    }

    /**
     * Copy bytes from <code>InputStream</code> to an <code>OutputStream</code>.
     * <p/>
     * This method uses the provided buffer, so there is no need to use a
     * <code>BufferedInputStream</code>.
     *
     * @param input  the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @param buffer the buffer to use for the copy
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     */
    public static void copy(InputStream input, OutputStream output, byte[] buffer) throws IOException {
        int n;
        while ((n = input.read(buffer)) > 0) {
            output.write(buffer, 0, n);
        }
    }
}
