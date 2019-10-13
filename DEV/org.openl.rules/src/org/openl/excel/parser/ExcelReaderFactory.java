package org.openl.excel.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.poifs.filesystem.FileMagic;
import org.openl.excel.parser.dom.DOMReader;
import org.openl.excel.parser.event.EventReader;
import org.openl.excel.parser.sax.SAXReader;
import org.openl.util.IOUtils;

public abstract class ExcelReaderFactory {

    public static ExcelReaderFactory sequentialFactory() {
        return new SequentialExcelReaderFactory();
    }

    public static ExcelReaderFactory fullReadFactory() {
        return new FullReadExcelReaderFactory();
    }

    /**
     * Use static create* methods instead
     */
    private ExcelReaderFactory() {
    }

    public final ExcelReader create(String fileName) {
        return create(fileName, null);
    }

    public final ExcelReader create(InputStream inputStream) {
        return create(null, inputStream);
    }

    protected abstract ExcelReader create(String fileName, InputStream inputStream);

    private static class SequentialExcelReaderFactory extends ExcelReaderFactory {
        @Override
        public ExcelReader create(String fileName, final InputStream is) {
            boolean useFile = fileName != null;

            if (useFile && is != null) {
                throw new IllegalArgumentException("Only one argument can be non-null.");
            }

            InputStream tempStream = null;
            try {
                tempStream = FileMagic.prepareToCheckMagic(useFile ? new FileInputStream(fileName) : is);

                // Opening the file by name is preferred because using an InputStream has a higher memory footprint than
                // using a File
                if (isXlsx(tempStream)) {
                    return useFile ? new SAXReader(fileName) : new SAXReader(tempStream);
                } else {
                    return useFile ? new EventReader(fileName) : new EventReader(tempStream);
                }
            } catch (IOException e) {
                throw new ExcelParseException(e);
            } finally {
                IOUtils.closeQuietly(tempStream);
            }
        }

        /**
         * Checking file extension is not enough: sometimes file has incorrect extension, sometimes we don't have file
         * name at all (only input stream).
         */
        private static boolean isXlsx(InputStream is) throws IOException {
            FileMagic fm = FileMagic.valueOf(is);

            switch (fm) {
                case OLE2:
                    return false;
                case OOXML:
                    return true;
                default:
                    throw new ExcelParseException("Unknown file format");
            }
        }
    }

    private static class FullReadExcelReaderFactory extends ExcelReaderFactory {
        @Override
        public ExcelReader create(String fileName, InputStream is) {
            return fileName != null ? new DOMReader(fileName) : new DOMReader(is);
        }
    }
}
