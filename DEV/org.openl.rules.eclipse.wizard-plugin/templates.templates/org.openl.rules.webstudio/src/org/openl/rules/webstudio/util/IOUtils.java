package org.openl.rules.webstudio.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;


/**
 * Helper methods related to I/O.
 *
 * @author Andrey Naumenko
 */
public class IOUtils {
    private static final Log log = LogFactory.getLog(IOUtils.class);

    /**
     * Close Reader, any exceptions are just logged.
     *
     * @param reader Reader, can be <code>null</code>.
     */
    public static void closeSilently(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                log.warn("Unable to close Reader", e);
            }
        }
    }

    /**
     * Close Writer, any exceptions are just logged.
     *
     * @param writer Writer, can be <code>null</code>.
     */
    public static void closeSilently(Writer writer) {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                log.warn("Unable to close Writer", e);
            }
        }
    }

    /**
     * Close InputStream, any exceptions are just logged.
     *
     * @param inputStream InputStream, can be <code>null</code>.
     */
    public static void closeSilently(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.warn("Unable to close InputStream", e);
            }
        }
    }

    /**
     * Close OutputStream, any exceptions are just logged.
     *
     * @param outputStream Writer, can be <code>null</code>.
     */
    public static void closeSilently(OutputStream outputStream) {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                log.warn("Unable to close OutputStream", e);
            }
        }
    }
}
