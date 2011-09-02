package org.openl.rules.lang.xls;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.openl.rules.indexer.IDocumentType;
import org.openl.rules.indexer.IIndexElement;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.source.impl.SourceCodeModuleDelegator;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;

public class XlsWorkbookSourceCodeModule extends SourceCodeModuleDelegator implements IIndexElement {

    private Workbook workbook;

	private Set<Short> wbColors = new TreeSet<Short>();

    private Collection<XlsWorkbookListener> listeners = new ArrayList<XlsWorkbookListener>();

    public XlsWorkbookSourceCodeModule(IOpenSourceCodeModule src) {
        this(src, loadWorkbook(src));
    }

    public XlsWorkbookSourceCodeModule(IOpenSourceCodeModule src, Workbook workbook) {
        super(src);
        this.workbook = workbook;
        if (workbook instanceof HSSFWorkbook) {
            initWorkbookColors();
        }
    }

    private static Workbook loadWorkbook(IOpenSourceCodeModule src) {
        InputStream is = null;
        try {
            is = src.getByteStream();
            return WorkbookFactory.create(is);
        } catch (Throwable t) {
            throw RuntimeExceptionWrapper.wrap(t);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }

            } catch (Throwable e) {
                Log.error("Error trying close input stream:", e);
            }
        }
    }

    private void initWorkbookColors() {
        short numStyles = workbook.getNumCellStyles();
        for (short i = 0; i < numStyles; i++) {
            CellStyle cellStyle = workbook.getCellStyleAt(i);

            wbColors.add(cellStyle.getFillForegroundColor());
            wbColors.add(cellStyle.getFillBackgroundColor());
            wbColors.add(cellStyle.getTopBorderColor());
            wbColors.add(cellStyle.getBottomBorderColor());
            wbColors.add(cellStyle.getLeftBorderColor());
            wbColors.add(cellStyle.getRightBorderColor());
        }

        short numFonts = workbook.getNumberOfFonts();
        for (short i = 0; i < numFonts; i++) {
            Font font = workbook.getFontAt(i);
            wbColors.add(font.getColor());
        }
    }

    public void addListener(XlsWorkbookListener listener) {
        listeners.add(listener);
    }

    public Collection<XlsWorkbookListener> getListeners() {
        return listeners;
    }

    public String getCategory() {
        return IDocumentType.WORKBOOK.getCategory();
    }

    public String getDisplayName() {
        String uri = src.getUri(0);

        try {
            URL url = new URL(uri);
            String file = url.getFile();
            int index = file.lastIndexOf('/');

            return index < 0 ? file : file.substring(index + 1);

        } catch (MalformedURLException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }

    }

    public String getIndexedText() {
        return getDisplayName();
    }

    public String getType() {
        return IDocumentType.WORKBOOK.getType();
    }

    public String getUri() {
        return src.getUri(0);
    }

    public Workbook getWorkbook() {
        return workbook;
    }

    public File getSourceFile() {
        File sourceFile = null;
        if (src instanceof FileSourceCodeModule) {
            sourceFile = ((FileSourceCodeModule) src).getFile();
        } else if (src instanceof URLSourceCodeModule) {
            sourceFile = new File(((URLSourceCodeModule) src).getUrl().getFile());
        } else {
            try {
                sourceFile = new File(new URI(getUri()));
            } catch (URISyntaxException me) {
                Log.warn("The xls source is not file based");
            }
        }
        return sourceFile;
    }

    public void save() throws IOException {
        String fileName = getSourceFile().getCanonicalPath();
        saveAs(fileName);
    }

    public void saveAs(String fileName) throws IOException {
        for (XlsWorkbookListener wl : listeners) {
            wl.beforeSave(this);
        }

        FileOutputStream fileOut = new FileOutputStream(fileName);
        workbook.write(fileOut);
        fileOut.close();

        for (XlsWorkbookListener wl : listeners) {
            wl.afterSave(this);
        }
        //workbook = loadWorkbook(src, false);
    }

    public IOpenSourceCodeModule getSource() {
        return src;
    }

    public Set<Short> getWorkbookColors() {
        return wbColors;
    }

}
