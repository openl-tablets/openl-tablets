package org.openl.rules.webstudio.services.upload;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.rules.webstudio.services.ServiceException;
import org.openl.rules.webstudio.util.FileUtils;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.rules.workspace.uw.UserWorkspaceProjectFolder;

import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Date;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
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
        UserWorkspace workspace = params.getWorkspace();
        UserWorkspaceProject project = null;
        try {
            workspace.createProject(params.getProjectName());
            project = workspace.getProject(params.getProjectName());
            project.checkOut();
        } catch (ProjectException e) {
            log.error("Error creating project", e);
            return;
        }

        Enumeration<?extends ZipEntry> files = zipFile.entries();

        String fileNameWithoutExt = FilenameUtils.getBaseName(params.getFile().getName());

        File uploadDir = getFile(params, fileNameWithoutExt);
        String prevItemName = null;
        UserWorkspaceProjectFolder prevFolder = project;

        Set<String> entries = new TreeSet<String>();
        for (Enumeration<?extends ZipEntry> items = files; items.hasMoreElements();) {
            ZipEntry item = items.nextElement();
            entries.add(item.getName());
        }

        for (String name : entries) {
            ZipEntry item = zipFile.getEntry(name);
            File targetFile = new File(uploadDir, item.getName()); // Determine file to save uploaded file

            StringBuilder itemName = new StringBuilder(item.getName());
            System.out.println(itemName);

            int pos = StringUtils.indexOf(itemName.toString(), prevItemName);

            StringBuilder shortItemName;
            if (pos == -1) {
                shortItemName = new StringBuilder(itemName.toString());
            } else {
                shortItemName = new StringBuilder(itemName.toString()
                            .substring(pos + prevItemName.length() + 1));
            }

            if (item.isDirectory()) {
                itemName.deleteCharAt(itemName.length() - 1);
                shortItemName.deleteCharAt(shortItemName.length() - 1);
                try {
                    UserWorkspaceProjectFolder folder;
                    if (pos != -1) {
                        folder = prevFolder.addFolder(shortItemName.toString());
                    } else {
                        folder = project.addFolder(itemName.toString());
                    }
                    prevItemName = itemName.toString();
                    prevFolder = folder;
                } catch (ProjectException e) {
                    log.error("Error adding folder to user workspace", e);
                    return;
                }
                targetFile.mkdirs();
            } else {
                FileUtils.createParentDirs(targetFile);
                InputStream zipInputStream = zipFile.getInputStream(item);

                ProjectResource projectResource = new FileProjectResource(zipInputStream);
                try {
                    prevFolder.addResource(shortItemName.toString(), projectResource);
                } catch (ProjectException e) {
                    log.error("Error adding file to user workspace", e);
                    return;
                }

                try {
                    FileCopyUtils.copy(zipInputStream, new FileOutputStream(targetFile));
                } finally {
                    if (zipInputStream != null) {
                        zipInputStream.close();
                    }
                }
            }
        }

        try {
            project.checkIn();
        } catch (ProjectException e) {
            log.error("Error during project checkIn", e);
            return;
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
