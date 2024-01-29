package org.openl.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A set of utils to work with general IO streams.
 *
 * @author Yury Molchan
 */
public class IOUtils {

    /**
     * Unconditionally close a <code>AutoCloseable</code>.
     * <p/>
     * Equivalent to {@link AutoCloseable#close()}, except any exceptions will be ignored.
     *
     * @param closeable the object to close, may be null or already closed
     */
    public static void closeQuietly(AutoCloseable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception ioe) {
            // ignore
        }
    }

    /**
     * Copy bytes from <code>InputStream</code> to an <code>OutputStream</code> and close them after.
     * <p/>
     *
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @throws NullPointerException if the input or output is null
     * @throws IOException if an I/O error occurs
     */
    public static void copyAndClose(InputStream input, OutputStream output) throws IOException {
        try {
            input.transferTo(output);
        } finally {
            closeQuietly(input);
            closeQuietly(output);
        }
    }

    /**
     * Copy bytes from <code>InputStream</code> to an <code>OutputStream</code>.
     * <p/>
     * This method uses the provided buffer, so there is no need to use a <code>BufferedInputStream</code>.
     *
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @param buffer the buffer to use for the copy
     * @throws NullPointerException if the input or output is null
     * @throws IOException if an I/O error occurs
     */
    public static void copy(InputStream input, OutputStream output, byte[] buffer) throws IOException {
        int n;
        while ((n = input.read(buffer)) > 0) {
            output.write(buffer, 0, n);
        }
        output.flush();
    }

    /**
     * Convert the specified CharSequence to an input stream, encoded as bytes using UTF-8 character encoding.
     *
     * @param input the CharSequence to convert
     * @return an input stream
     */
    public static InputStream toInputStream(CharSequence input) {
        return new ByteArrayInputStream(input.toString().getBytes(StandardCharsets.UTF_8));
    }
}
