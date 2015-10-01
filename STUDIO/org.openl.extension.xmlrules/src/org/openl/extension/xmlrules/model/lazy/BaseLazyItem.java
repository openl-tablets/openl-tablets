package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.openl.extension.xmlrules.model.single.XlsRegionImpl;
import org.openl.util.IOUtils;

public abstract class BaseLazyItem<T> {
    private final File file;
    private final String entryName;
    private WeakReference<T> info = new WeakReference<T>(null);

    public BaseLazyItem(File file, String entryName) {
        this.file = file;
        this.entryName = entryName;
    }

    protected String getEntryName() {
        return entryName;
    }

    protected File getFile() {
        return file;
    }

    protected T getInfo() {
        T item = info.get();
        if (item == null) {
            item = deserializeInfo();
            postProcess(item);
            info = new WeakReference<T>(item);
        }

        return item;
    }

    protected void postProcess(T info) {
        // Do nothing
    }

    protected void postProcess(XlsRegionImpl path) {
        if (path == null) {
            return;
        }
        if (path.getWidth() == null) {
            path.setWidth(1);
        }
        if (path.getHeight() == null) {
            path.setHeight(1);
        }
    }

    protected T deserializeInfo() {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
            ZipEntry entry = zipFile.getEntry(entryName);
            if (entry == null || entry.isDirectory()) {
                return null;
            }

            InputStream inputStream = zipFile.getInputStream(entry);
            try {
                JAXBContext context = JAXBContext.newInstance("org.openl.extension.xmlrules.model.single");
                Unmarshaller m = context.createUnmarshaller();
                @SuppressWarnings("unchecked")
                T item = (T) m.unmarshal(new InputStreamReader(inputStream, "UTF-8"));
                return item;
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        } catch (JAXBException e) {
            throw new IllegalStateException("Can't deserialize the file: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException("Can't deserialize the file: " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(zipFile);
        }
    }
}
