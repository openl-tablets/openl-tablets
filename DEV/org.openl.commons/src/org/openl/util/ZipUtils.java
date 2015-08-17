package org.openl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A utility class to work with zip files.
 *
 * @author Yury Molchan
 */
public class ZipUtils {

    /**
     * Extract all files from a zip file into a directory.
     *
     * @param zipFile      input zip file
     * @param outputFolder zip file output folder
     */
    public static void extractAll(File zipFile, File outputFolder) throws IOException {

        byte[] buffer = new byte[8192];

        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        try {
            //get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {

                String fileName = ze.getName();
                File unzipped = new File(outputFolder, fileName);

                //create all non exists folders
                new File(unzipped.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(unzipped);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                ze = zis.getNextEntry();
            }
        } finally {
            zis.closeEntry();
            zis.close();
        }

    }
}

