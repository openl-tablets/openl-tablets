package org.openl.rules.webstudio.services.upload;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.rules.webstudio.services.ServiceException;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.rules.workspace.uw.UserWorkspaceProjectFolder;

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

            uploadFiles(params, result, zipFile);
        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
        }
    }

    private void uploadFiles(UploadServiceParams params, UploadServiceResult result,
        ZipFile zipFile) throws ServiceException, IOException
    {
        UserWorkspace workspace = params.getWorkspace();
        UserWorkspaceProject project = null;
        try {
            workspace.createProject(params.getProjectName());
            project = workspace.getProject(params.getProjectName());
            project.checkOut();
        } catch (ProjectException e) {
            throw new ServiceException("Error creating project", e);
        }

        String fileNameWithoutExt = FilenameUtils.getBaseName(params.getFile().getName());
        File uploadDir = getFile(params, fileNameWithoutExt);

        // Sort zip entries names alphabetically
        Set<String> sortedNames = new TreeSet<String>();
        for (Enumeration<?extends ZipEntry> items = zipFile.entries();
                items.hasMoreElements();) {
            ZipEntry item = items.nextElement();
            sortedNames.add(item.getName());
        }

        UserWorkspaceProjectFolder prevFolder = null;
        String prevItemName = null;

        for (String name : sortedNames) {
            ZipEntry item = zipFile.getEntry(name);

            //File targetFile = new File(uploadDir, item.getName()); // Determine file to save uploaded file
            String fullName = item.getName();

            int pos = StringUtils.indexOf(fullName, prevItemName);

            String shortName;
            if (pos != -1) {
                shortName = fullName.substring(pos + prevItemName.length() + 1);
            } else {
                shortName = fullName;
                prevFolder = project;
            }

            if (item.isDirectory()) {
                fullName = fullName.substring(0, fullName.length() - 1);
                shortName = shortName.substring(0, shortName.length() - 1);
                try {
                    prevFolder = prevFolder.addFolder(shortName);
                    prevItemName = fullName;
                } catch (ProjectException e) {
                    throw new ServiceException("Error adding folder to user workspace", e);
                }

                //targetFile.mkdirs();
            } else {
                //FileUtils.createParentDirs(targetFile);
                InputStream zipInputStream = zipFile.getInputStream(item);

                ProjectResource projectResource = new FileProjectResource(zipInputStream);
                try {
                    prevFolder.addResource(shortName.toString(), projectResource);
                } catch (ProjectException e) {
                    throw new ServiceException("Error adding file to user workspace", e);
                }

                /*
                   try {
                       FileCopyUtils.copy(zipInputStream, new FileOutputStream(targetFile));
                   } finally {
                       if (zipInputStream != null) {
                           zipInputStream.close();
                       }
                   }
                 */
            }
        }

        try {
            project.checkIn();
        } catch (ProjectException e) {
            throw new ServiceException("Error during project checkIn", e);
        }

        result.setResultFile(uploadDir);
    }
}
