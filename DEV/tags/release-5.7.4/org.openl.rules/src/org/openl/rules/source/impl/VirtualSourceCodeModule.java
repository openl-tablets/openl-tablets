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

    private static final String SOURCE_URI = "<virtual_uri>";
    
    private Workbook workbook = new HSSFWorkbook();

    @Override
    protected String makeUri() {
        return SOURCE_URI;
    }

    public InputStream getByteStream() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Reader getCharacterStream() {
        return new InputStreamReader(getByteStream());
    } 
}
