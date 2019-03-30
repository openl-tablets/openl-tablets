package org.openl.excel.parser.event;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.openl.excel.parser.ExcelParseException;
import org.openl.excel.parser.ExcelReader;
import org.openl.excel.parser.SheetDescriptor;
import org.openl.excel.parser.TableStyles;
import org.openl.rules.table.IGridRegion;
import org.openl.util.FileTool;
import org.openl.util.FileUtils;

public class EventReader implements ExcelReader {
    private final String fileName;
    private File tempFile;
    private WorkbookListener listener;

    public EventReader(String fileName) {
        this.fileName = fileName;
    }

    public EventReader(InputStream is) {
        // Save to temp file because using an InputStream has a higher memory footprint than using a File. See POI javadocs.
        tempFile = FileTool.toTempFile(is, "stream.xls");
        this.fileName = tempFile.getAbsolutePath();
    }

    @Override
    public List<? extends SheetDescriptor> getSheets() {
        if (listener == null) {
            initialize();
        }
        return listener.getSheets();
    }

    @Override
    public Object[][] getCells(SheetDescriptor sheet) {
        if (listener == null) {
            initialize();
        }
        return listener.getCells(sheet);
    }

    @Override
    public boolean isUse1904Windowing() {
        if (listener == null) {
            initialize();
        }
        return listener.isUse1904Windowing();
    }

    @Override
    public TableStyles getTableStyles(SheetDescriptor sheet, IGridRegion tableRegion) {
        try {
            TableStyleListener listener = new TableStyleListener((EventSheetDescriptor) sheet, tableRegion);
            listener.process(fileName);

            return listener.getTableStyles();
        } catch (java.io.IOException e) {
            throw new ExcelParseException(e);
        }
    }

    private void initialize() {
        try {
            WorkbookListener workbookListener = new WorkbookListener();
            workbookListener.process(fileName);
            this.listener = workbookListener;
        } catch (java.io.IOException e) {
            throw new ExcelParseException(e);
        }
    }

    @Override
    public void close() {
        listener = null;

        FileUtils.deleteQuietly(tempFile);
        tempFile = null;
    }

}
