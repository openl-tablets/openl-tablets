package org.openl.rules.webstudio.services.upload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.webstudio.services.ServiceException;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.abstracts.impl.ArtefactPathImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.rules.workspace.uw.UserWorkspaceProjectFolder;


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
        UserWorkspaceProject project;
        try {
            workspace.createProject(params.getProjectName());
            project = workspace.getProject(params.getProjectName());
            project.checkOut();
        } catch (ProjectException e) {
            throw new ServiceException("Error creating project", e);
        }

        String fileNameWithoutExt = FilenameUtils.getBaseName(params.getFile().getName());
        File uploadDir = getFile(params, fileNameWithoutExt);
        UploadFilter filter = getFilter();

        // Sort zip entries names alphabetically
        Set<String> sortedNames = new TreeSet<String>();
        for (Enumeration<?extends ZipEntry> items = zipFile.entries(); items.hasMoreElements();) {
            ZipEntry item = items.nextElement();
            if (filter.accept(item.getName()))
                sortedNames.add(item.getName());
        }

        for (String name : sortedNames) {
            ZipEntry item = zipFile.getEntry(name);

            //File targetFile = new File(uploadDir, item.getName()); // Determine file to save uploaded file
            String fullName = item.getName();

            if (item.isDirectory()) {
                fullName = fullName.substring(0, fullName.length() - 1);

                checkPath(project, fullName);

                //targetFile.mkdirs();
            } else {
                //FileUtils.createParentDirs(targetFile);
                InputStream zipInputStream = zipFile.getInputStream(item);

                UserWorkspaceProjectFolder folder = project;
                String resName;

                int pos = fullName.lastIndexOf('/');
                if (pos >=0) {
                    String path = fullName.substring(0, pos);
                    resName = fullName.substring(pos + 1);

                    folder = checkPath(project, path);
                } else {
                    resName = fullName;
                }


                ProjectResource projectResource = new FileProjectResource(zipInputStream);
                try {
                    folder.addResource(resName, projectResource);
                } catch (ProjectException e) {
                    throw new ServiceException("Error adding file to user workspace", e);
                }
            }
        }

        try {
            project.checkIn();
        } catch (ProjectException e) {
            throw new ServiceException("Error during project checkIn", e);
        }

        result.setResultFile(uploadDir);
    }

    private UserWorkspaceProjectFolder checkPath(UserWorkspaceProject project, String fullName) throws ServiceException {
        ArtefactPathImpl ap = new ArtefactPathImpl(fullName);
        UserWorkspaceProjectFolder current = project;
        for (String segment : ap.getSegments()) {
            if (current.hasArtefact(segment)) {
                try {
                    current = (UserWorkspaceProjectFolder) current.getArtefact(segment);
                } catch (ProjectException e) {
                    throw new ServiceException("Cannot get user workspace folder " + segment, e);
                }
            } else {
                try {
                    current = current.addFolder(segment);
                } catch (ProjectException e) {
                    throw new ServiceException("Error adding folder " + segment + " to user workspace", e);
                }
            }
        }

        return current;
    }
}
