package org.openl.rules.project.resolving;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.validation.ValidationException;

public class ProjectDescriptorBasedResolvingStrategy extends BaseResolvingStrategy {

    private final Log log = LogFactory.getLog(ProjectDescriptorBasedResolvingStrategy.class);

    public final static String PROJECT_DESCRIPTOR_FILE_NAME = "rules.xml";

    public boolean isRulesProject(File folder) {
        File descriptorFile = new File(folder, PROJECT_DESCRIPTOR_FILE_NAME);
        if (descriptorFile.exists()) {
            log.debug(String.format("Project in %s folder was resolved as Project descriptor based project",
                folder.getPath()));
            return true;
        } else {
            log.debug(String.format("Project descriptor based strategy failed to resolve project folder %s:" + "there is no file %s in folder",
                folder.getPath(),
                PROJECT_DESCRIPTOR_FILE_NAME));
            return false;
        }
    }

    protected ProjectDescriptor internalResolveProject(File folder) throws ProjectResolvingException {
        File descriptorFile = new File(folder, PROJECT_DESCRIPTOR_FILE_NAME);
        ProjectDescriptorManager descriptorManager = new ProjectDescriptorManager();
        try {
            return descriptorManager.readDescriptor(descriptorFile);
        } catch (ValidationException ex) {
            throw new ProjectResolvingException("Project descriptor is invalid. Please, verify " + PROJECT_DESCRIPTOR_FILE_NAME + " file format.",
                ex);
        } catch (FileNotFoundException e) {
            throw new ProjectResolvingException("Project descriptor wasn't found! Project should countain " + PROJECT_DESCRIPTOR_FILE_NAME + " file.",
                e);
        } catch (Exception e) {
            throw new ProjectResolvingException("Project descriptor reading failed.", e);
        }
    }

}
