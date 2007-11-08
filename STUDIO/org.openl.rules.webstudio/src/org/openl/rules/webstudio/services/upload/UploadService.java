package org.openl.rules.webstudio.services.upload;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.rules.webstudio.services.ServiceException;
import org.openl.rules.webstudio.util.FileUtils;


import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;


/**
 * Upload service.
 *
 * @author Andrey Naumenko
 */
public class UploadService extends BaseUploadService {
    private final static Log log = LogFactory.getLog(UploadService.class);
    private static final int LISTING_SIZE = 5;

    /**
     * {@inheritDoc}
     */
    protected File getFile(UploadServiceParams params, String fileName)
        throws IOException
    {
        File file = new File("uploadedProjects/" + fileName);
        if (file.exists()) {
            long time = new Date().getTime();
            int endIndex = fileName.lastIndexOf(".");
            if (endIndex == -1) {
                fileName = fileName + "-" + time;
            } else {
                fileName = fileName.substring(0, endIndex) + "-" + time
                    + fileName.substring(endIndex);
            }
            file = new File("uploadedProjects/" + fileName);
        }
        //FileUtils.createParentDirs(file);
        return file;
    }

    /**
     * {@inheritDoc}
     */
    protected void unpack(UploadServiceParams params, UploadServiceResult result,
        File tempFile) throws IOException, ServiceException
    {
        if (log.isDebugEnabled()) {
            log.debug("Unpacking zip file ");
        }

        //unpack uploaded zip file
        ZipFile zipFile = null;
        try {
            try {
                zipFile = new ZipFile(tempFile);
            } catch (IOException e) {
                throw new NotUnzippedFileException("File '" + params.getFile().getName()
                    + "' is not a zip or it is corrupt.");
            }

            Enumeration<?extends ZipEntry> files = zipFile.entries();

            int fileCount = 0;
            int dirCount = 0;

            for (Enumeration<?extends ZipEntry> items = files; items.hasMoreElements();) {
                ZipEntry item = items.nextElement();
                if (item.isDirectory()) {
                    dirCount++;
                    continue;
                } else {
                    fileCount++;
                }
            }

            if ((fileCount == 0) && (dirCount == 0)) {
                WrongNumberOfZipEntriesException e = new WrongNumberOfZipEntriesException("Zip file not contain any file",
                        "none");
                e.setFileCount(0);
                e.setListing(generateListing(zipFile.entries()));
                throw e;
            }

            if (fileCount == 0) {
                WrongNumberOfZipEntriesException e = new WrongNumberOfZipEntriesException("Zip file contains only directories",
                        "directory");
                e.setFileCount(0);
                e.setListing(generateListing(zipFile.entries()));
                throw e;
            }

            uploadManyFiles(params, result, zipFile);
        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
        }
    }

    private void uploadManyFiles(UploadServiceParams params, UploadServiceResult result,
        ZipFile zipFile) throws IOException, ZipException, FileNotFoundException
    {
        Enumeration<?extends ZipEntry> files = zipFile.entries();

        List<File> rememberFiles = new ArrayList<File>();
        String fileNameWithoutExt = FilenameUtils.getBaseName(params.getFile().getName());

        File uploadDir = getFile(params, fileNameWithoutExt);

        for (Enumeration<?extends ZipEntry> items = files; items.hasMoreElements();) {
            ZipEntry item = items.nextElement();

            File targetFile = new File(uploadDir, item.getName()); // Determine file to save uploaded file
            if (item.isDirectory()) {
                targetFile.mkdirs();
            } else {
                FileUtils.createParentDirs(targetFile);
                rememberFiles.add(targetFile);
                InputStream zipInputStream = zipFile.getInputStream(item);

                try {
                    // copy file to <supplier workarea>/uploads
                    FileCopyUtils.copy(zipInputStream, new FileOutputStream(targetFile));
                } finally {
                    if (zipInputStream != null) {
                        zipInputStream.close();
                    }
                }
            }
        }
        result.setResultFile(uploadDir);
    }

    private String generateListing(Enumeration<?extends ZipEntry> entries) {
        StringBuffer s = new StringBuffer("");
        int count = 0;
        for (; entries.hasMoreElements();) {
            ZipEntry element = (ZipEntry) entries.nextElement();
            if (!element.isDirectory()) {
                if (s.length() != 0) {
                    s.append(", ");
                }
                if (count == LISTING_SIZE) {
                    s.append("...");
                } else {
                    s.append(element.getName());
                }
                count++;
            }

            if (count > LISTING_SIZE) {
                break;
            }
        }

        return s.toString();
    }
}
