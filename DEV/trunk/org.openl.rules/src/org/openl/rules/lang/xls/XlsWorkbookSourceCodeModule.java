package org.openl.rules.lang.xls;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.EventListener;
import java.util.Collection;
import java.util.ArrayList;

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

    public interface WorkbookListener extends EventListener {
        void beforeSave(XlsWorkbookSourceCodeModule xwscm);
    }

	private Workbook workbook;

    private Collection<WorkbookListener> listeners = new ArrayList<WorkbookListener>();

    /*raised for test*/ File sourceFile;
    private long lastModified;

    public XlsWorkbookSourceCodeModule(IOpenSourceCodeModule src) {
        this(src, loadWorkbook(src));
    }

    public XlsWorkbookSourceCodeModule(IOpenSourceCodeModule src, Workbook workbook) {
        super(src);
        this.workbook = workbook;
        initSourceFile();
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

    private void initSourceFile() {
        try {
            sourceFile = getFile();
            lastModified = sourceFile.lastModified();
        } catch (Exception e) {
            Log.error("Error when trying to get source file", e);
        }
    }

    public void addListener(WorkbookListener listener) {
        listeners.add(listener);
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

    private File getFile() throws IOException {
        File sourceFile = null;
        if (src instanceof FileSourceCodeModule) {
            sourceFile = ((FileSourceCodeModule) src).getFile();
        } else if (src instanceof URLSourceCodeModule) {
            sourceFile = new File(((URLSourceCodeModule) src).getUrl().getFile());
        } else {
            try {
                sourceFile = new File(new URI(getUri()));
            } catch (URISyntaxException me) {
                throw new IOException("The xls source is not file based");
            }
        }
        return sourceFile;
    }

    public boolean isModified() {
        if (sourceFile == null) {
            Log.warn(String.format("Undefined source file for [%s]", getUri()));
            return false;
        }
        return sourceFile.lastModified() != lastModified;
    }

    public void save() throws IOException {
        String fileName = sourceFile.getCanonicalPath();
        saveAs(fileName);
    }

    public void saveAs(String fileName) throws IOException {
        for (WorkbookListener wl : listeners) {
            wl.beforeSave(this);
        }
        FileOutputStream fileOut = new FileOutputStream(fileName);
        workbook.write(fileOut);
        fileOut.close();
        //workbook = loadWorkbook(src, false);
    }

}
