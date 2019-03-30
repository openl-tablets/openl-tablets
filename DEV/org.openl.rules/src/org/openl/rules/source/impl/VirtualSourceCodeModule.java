package org.openl.rules.source.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.source.impl.ASourceCodeModule;

public class VirtualSourceCodeModule extends ASourceCodeModule {

    public static final String SOURCE_URI = "<virtual_uri>";
    public static final String VIRTUAL_SHEET_NAME = "$virtual_sheet$";

    private Workbook workbook;

    public VirtualSourceCodeModule() {
        workbook = new HSSFWorkbook();
        workbook.createSheet(VIRTUAL_SHEET_NAME);
    }

    @Override
    protected String makeUri() {
        return SOURCE_URI;
    }

    @Override
    public InputStream getByteStream() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Reader getCharacterStream() {
        return new InputStreamReader(getByteStream());
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void resetModified() {
    }
}
