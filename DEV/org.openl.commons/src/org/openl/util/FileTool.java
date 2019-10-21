package org.openl.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileTool {

    public static File buildRelativePath(File startDir, File targetFile) throws IOException {
        if (startDir == null) {
            return targetFile.isFile() ? targetFile.getParentFile() : targetFile;
        }

        if (startDir.equals(targetFile)) {
            return new File(".");
        }

        File[] pfDir = parents(startDir);
        File[] pfTarget = parents(targetFile);

        int size = Math.min(pfDir.length, pfTarget.length);

        int lastEqual = -1;
        for (int i = 0; i < size; i++) {
            if (pfDir[i].equals(pfTarget[i])) {
                lastEqual = i;
            }
        }

        if (lastEqual == -1) {
            return targetFile.getAbsoluteFile();
        }

        int stepsToCommonParent = pfDir.length - 1 - lastEqual;

        StringBuilder path = new StringBuilder(".");

        for (int i = 0; i < stepsToCommonParent; ++i) {
            if (i == 0) {
                path = new StringBuilder("..");
            } else {
                path.append("/..");
            }
        }

        stepsToCommonParent = pfTarget.length - 1 - lastEqual;

        for (int i = 0; i < stepsToCommonParent; i++) {
            path.append("/").append(pfTarget[lastEqual + i + 1].getName());
        }

        return new File(path.toString());

    }

    static File[] parents(File f) throws IOException {

        f = f.getCanonicalFile();
        List<File> v = new ArrayList<>();
        v.add(f);

        while ((f = f.getParentFile()) != null) {
            v.add(f.getCanonicalFile());
        }

        int size = v.size();
        File[] ff = new File[size];
        for (int i = 0; i < size; i++) {
            ff[i] = v.get(size - 1 - i);
        }

        return ff;

    }

    public static File toTempFile(InputStream source, String fileName) {
        File file = null;
        try {
            file = File.createTempFile(fileName, null);
            IOUtils.copyAndClose(source, new FileOutputStream(file));
        } catch (IOException e) {
            final Logger log = LoggerFactory.getLogger(FileTool.class);
            log.error("Failed to create a file: {}", fileName, e);
        }
        return file;
    }
}
