package org.openl.rules.workspace.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtil {
    private ZipUtil() {}

    public static void zipFolder(File folder, File zipFilename) throws IOException {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFilename));
        out.setLevel(Deflater.DEFAULT_COMPRESSION);

        new ZipUtil().zipFolder(folder, new File(""), out);

        out.close();
    }

    public static void unzip(File zipFilename, File destFolder) throws IOException {
        ZipInputStream in = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFilename)));
        try {
            ZipEntry entry;
            final int BUFFER = 1 << 16;
            byte[] buffer = new byte[BUFFER];
            while ((entry = in.getNextEntry()) != null) {
                File destFile = new File(destFolder, entry.getName());
                destFile.getParentFile().mkdirs();
                OutputStream dest = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER);
                IOUtil.copy(in, dest, buffer);
            }
        } finally {
            in.close();
        }
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
                InputStream in = new BufferedInputStream(new FileInputStream(f));
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
