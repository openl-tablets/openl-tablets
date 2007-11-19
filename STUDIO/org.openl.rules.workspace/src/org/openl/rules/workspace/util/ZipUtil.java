package org.openl.rules.workspace.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil {
    private ZipUtil() {}

    public static void zipFolder(File folder, File zipFilename) throws IOException {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFilename));
        out.setLevel(Deflater.DEFAULT_COMPRESSION);

        new ZipUtil().zipFolder(folder, new File(""), out);

        out.close();
    }


    private byte[] buffer = new byte[1 << 16];

    private void zipFolder(File folder, File base, ZipOutputStream out) throws IOException {
        File[] files = folder.listFiles();
        int bytesRead;

        for (File f : files) {
            File entryFile = new File(base, f.getName());
            if (f.isDirectory()) {
                zipFolder(f, entryFile, out);
            } else {
                FileInputStream in = new FileInputStream(f);
                try {
                    ZipEntry entry = new ZipEntry(entryFile.getPath());
                    out.putNextEntry(entry);
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                } finally {
                    in.close();
                }
            }
        }
    }
}
