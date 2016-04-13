package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.openl.extension.xmlrules.ProjectData;
import org.openl.util.IOUtils;

public abstract class BaseLazyItem<T> {
    private static final String OPENL_FOLDER = ".openl/";
    private final File file;
    private final String entryName;
    private WeakReference<T> instance = new WeakReference<T>(null);

    private String prefix = OPENL_FOLDER;

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

    protected T getInstance() {
        T item = instance.get();
        if (item == null) {
            item = deserializeInfo();
            postProcess(item);
            instance = new WeakReference<T>(item);
        }

        return item;
    }

    protected void postProcess(T info) {
        // Do nothing
    }

    protected T deserializeInfo() {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
            ZipEntry entry = zipFile.getEntry(prefix + entryName);
            if (entry == null && !prefix.isEmpty()) {
                // Fallback to the case when all files in the root folder
                entry = zipFile.getEntry(entryName);
                prefix = "";
            }
            if (entry == null || entry.isDirectory()) {
                throw new IllegalStateException("Incorrect file format. The file '" + entryName + "' doesn't exist in the project.");
            }

            InputStream inputStream = zipFile.getInputStream(entry);
            try {
                Unmarshaller m = ProjectData.getUnmarshaller();
                @SuppressWarnings("unchecked")
                T item = (T) m.unmarshal(new InputStreamReader(inputStream, "UTF-8"));
                return item;
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        } catch (JAXBException e) {
            throw new IllegalStateException("Incorrect file format. Reason: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException("Can't read the file. Reason: " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(zipFile);
        }
    }
}
