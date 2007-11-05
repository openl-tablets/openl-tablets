package org.openl.rules.ui.repository;

import org.apache.myfaces.custom.fileupload.UploadedFile;


/**
 * Project upload controller.
 *
 * @author Andrey Naumenko
 */
public class ProjectUploadController {
    private UploadedFile file;
    private String projectName;

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String upload() {
        System.out.println(file.getName());

        //file.getInputStream();
        return null;
    }
}
