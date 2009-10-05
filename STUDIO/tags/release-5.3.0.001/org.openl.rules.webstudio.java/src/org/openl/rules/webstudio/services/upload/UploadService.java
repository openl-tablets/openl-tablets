package org.openl.rules.webstudio.services.upload;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.rules.webstudio.services.ServiceException;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.security.Privileges;
import org.acegisecurity.annotation.Secured;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.Date;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Upload service.
 *
 * @author Andrey Naumenko
 */
public class UploadService extends BaseUploadService {
    private final static Log log = LogFactory.getLog(UploadService.class);
    private PathFilter zipFilter;

    /**
     * {@inheritDoc}
     */
    @Override
    protected File getFile(UploadServiceParams params, String fileName) throws IOException {
        File file = new File("uploadedProjects/" + fileName);
        if (file.exists()) {
            long time = new Date().getTime();
            int endIndex = fileName.lastIndexOf(".");
            if (endIndex == -1) {
                fileName = fileName + "-" + time;
            } else {
                fileName = fileName.substring(0, endIndex) + "-" + time + fileName.substring(endIndex);
            }
            file = new File("uploadedProjects/" + fileName);
        }

        // FileUtils.createParentDirs(file);
        return file;
    }

    public void setZipFilter(PathFilter zipFilter) {
        this.zipFilter = zipFilter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Secured(Privileges.PRIVILEGE_CREATE)
    protected void unpack(UploadServiceParams params, UploadServiceResult result, File tempFile) throws IOException,
            ServiceException {
        if (log.isDebugEnabled()) {
            log.debug("Unpacking zip file");
        }

        // unpack uploaded zip file
        ZipFile zipFile = null;
        try {
            try {
                zipFile = new ZipFile(tempFile);
            } catch (IOException e) {
                throw new NotUnzippedFileException("File '" + params.getFile().getName()
                        + "' is not a ZIP or it is corrupted.");
            }

            uploadFiles(params, result, zipFile);
        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
        }
    }

    private void uploadFiles(UploadServiceParams params, UploadServiceResult result, ZipFile zipFile)
            throws ServiceException, IOException {
        RProjectBuilder builder;
        try {
            builder = new RProjectBuilder(params.getWorkspace(), params.getProjectName(), zipFilter);
        } catch (ProjectException e) {
            throw new ServiceException("Error creating project: " + e.getMessage(), e);
        }

        String fileNameWithoutExt = FilenameUtils.getBaseName(params.getFile().getName());
        File uploadDir = getFile(params, fileNameWithoutExt);

        // Sort zip entries names alphabetically
        Set<String> sortedNames = new TreeSet<String>();
        for (Enumeration<? extends ZipEntry> items = zipFile.entries(); items.hasMoreElements();) {
            ZipEntry item = items.nextElement();
            sortedNames.add(item.getName());
        }

        for (String name : sortedNames) {
            ZipEntry item = zipFile.getEntry(name);

            if (item.isDirectory()) {
                try {
                    builder.addFolder(item.getName());
                } catch (ProjectException e) {
                    builder.cancel();
                    throw new ServiceException("Error adding folder to user workspace: " + e.getMessage(), e);
                }
            } else {
                InputStream zipInputStream = zipFile.getInputStream(item);

                try {
                    builder.addFile(item.getName(), zipInputStream);
                } catch (ProjectException e) {
                    builder.cancel();
                    throw new ServiceException("Error adding file to user workspace: " + e.getMessage(), e);
                }
            }
        }

        try {
            builder.checkIn();
        } catch (ProjectException e) {
            throw new ServiceException("Error during project checkIn: " + e.getMessage(), e);
        }

        result.setResultFile(uploadDir);
    }
}
