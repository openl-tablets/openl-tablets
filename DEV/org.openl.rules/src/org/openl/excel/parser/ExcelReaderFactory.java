package org.openl.excel.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.poifs.filesystem.FileMagic;

import org.openl.excel.parser.dom.DOMReader;
import org.openl.excel.parser.event.EventReader;
import org.openl.excel.parser.markdown.MarkdownReader;
import org.openl.excel.parser.sax.SAXReader;
import org.openl.util.FileTypeHelper;

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

    /**
     * Creates a reader of a file. A Markdown module is told by its extension, an Excel workbook by its content.
     */
    public final ExcelReader create(String fileName) {
        if (!FileTypeHelper.isMarkdownFile(fileName)) {
            return create(fileName, null);
        }
        try {
            return create(Files.newInputStream(Path.of(fileName)), fileName);
        } catch (IOException e) {
            throw new ExcelParseException(e);
        }
    }

    public final ExcelReader create(InputStream inputStream) {
        return create(null, inputStream);
    }

    /**
     * Creates a reader of a source available only as a stream, such as an entry of an archive.
     *
     * <p>The content tells an Excel workbook, but not a Markdown file: that one is plain text, so its name tells
     * it instead. A Markdown file is read whole at once, and its stream is closed.
     *
     * @param sourceName the file name, path or URI of the source
     */
    public final ExcelReader create(InputStream inputStream, String sourceName) {
        if (!FileTypeHelper.isMarkdownFile(sourceName)) {
            return create(inputStream);
        }
        // A Markdown file is read whole when its reader is created, so the stream is done with, as for a workbook
        try (inputStream) {
            return new MarkdownReader(inputStream, sourceName);
        } catch (IOException e) {
            throw new ExcelParseException(e);
        }
    }

    protected abstract ExcelReader create(String fileName, InputStream inputStream);

    private static class SequentialExcelReaderFactory extends ExcelReaderFactory {
        @Override
        public ExcelReader create(String fileName, final InputStream is) {
            var useFile = fileName != null;

            if (useFile && is != null) {
                throw new IllegalArgumentException("Only one argument can be non-null.");
            }

            try (var tempStream = FileMagic.prepareToCheckMagic(useFile ? new FileInputStream(fileName) : is)) {
                // Opening the file by name is preferred because using an InputStream has a higher memory footprint than
                // using a File
                if (isXlsx(tempStream)) {
                    return useFile ? new SAXReader(fileName) : new SAXReader(tempStream);
                } else {
                    return useFile ? new EventReader(fileName) : new EventReader(tempStream);
                }
            } catch (IOException e) {
                throw new ExcelParseException(e);
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
