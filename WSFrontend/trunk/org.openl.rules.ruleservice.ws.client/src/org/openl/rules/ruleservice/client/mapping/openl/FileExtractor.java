package org.openl.rules.ruleservice.client.mapping.openl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Copies specified resource file to temp file, temp file is deleted when
 * virtual machine terminates.
 * 
 * @author Ivan Holub
 */
public class FileExtractor {

    private static final Log LOG = LogFactory.getLog(FileExtractor.class);

    private static final String TEMP_FILE_PREFIX = "mapping";

    private static final String TEMP_FILE_SUFFIX = ".xls";

    private FileExtractor() {
    }

    /**
     * Copies specified resource file to temp file, temp file is deleted when
     * virtual machine terminates. Returns the file as {@link File}.
     * 
     * @param mapperClass the class on which
     *            {@link Class#getResourceAsStream(String)} is called to
     *            retrieve resource
     * @param pathInJar path to resource
     * @return the file {@link File}
     * @throws IOException in case of error
     */
    public static File extractFile(@SuppressWarnings("rawtypes") Class mapperClass, String pathInJar)
            throws IOException {
        InputStream is = null;
        FileOutputStream out = null;
        try {
            File tempFile = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
            tempFile.deleteOnExit();
            is = mapperClass.getResourceAsStream(pathInJar);
            out = new FileOutputStream(tempFile);
            IOUtils.copy(is, out);
            return tempFile;
        } finally {
            closeQuietly(is);
            closeQuietly(out);
        }
    }

    private static void closeQuietly(InputStream input) {
        try {
            if (input != null) {
                input.close();
            }
        } catch (IOException e) {
            LOG.error("Error closing input stream", e);
        }
    }

    private static void closeQuietly(OutputStream output) {
        try {
            if (output != null) {
                output.close();
            }
        } catch (IOException e) {
            LOG.error("Error closing output stream", e);
        }
    }

}
