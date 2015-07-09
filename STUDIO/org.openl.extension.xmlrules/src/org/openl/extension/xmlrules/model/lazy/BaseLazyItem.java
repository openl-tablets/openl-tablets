package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.IOUtils;
import org.openl.extension.xmlrules.model.single.XlsRegionImpl;

public abstract class BaseLazyItem<T> {
    private final XStream xstream;
    private final File file;
    private final String entryName;
    private WeakReference<T> info = new WeakReference<T>(null);

    public BaseLazyItem(XStream xstream, File file, String entryName) {
        this.xstream = xstream;
        this.file = file;
        this.entryName = entryName;
    }

    protected XStream getXstream() {
        return xstream;
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
                @SuppressWarnings("unchecked")
                T item = (T) xstream.fromXML(inputStream);
                return item;
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Can't deserialize the file", e);
        } finally {
            IOUtils.closeQuietly(zipFile);
        }
    }
}
