package org.openl.rules.project.resolving;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.model.ProjectDescriptor;

public class ProjectDescriptorBasedResolvingStrategy implements ResolvingStrategy {
    
    private static final Log LOG = LogFactory.getLog(ProjectDescriptorBasedResolvingStrategy.class);

    public final static String PROJECT_DESCRIPTOR_FILE_NAME = "rules.xml";

    public boolean isRulesProject(File folder) {

        File descriptorFile = new File(folder, PROJECT_DESCRIPTOR_FILE_NAME);
        if (descriptorFile.exists()) {
            LOG.debug(String.format(
                "Project in %s folder was resolved as Project descriptor based project", folder.getPath()));
            return true;
        } else {
            LOG.debug(String.format("Project descriptor based strategy failed to resolve project folder %s:" +
            		"there is no file %s in folder", folder.getPath(), PROJECT_DESCRIPTOR_FILE_NAME));
            return false;
        }
    }

    public ProjectDescriptor resolveProject(File folder) {

        File descriptorFile = new File(folder, PROJECT_DESCRIPTOR_FILE_NAME);
        ProjectDescriptorManager descriptorManager = new ProjectDescriptorManager();

        try {
            return descriptorManager.readDescriptor(descriptorFile);
        } catch (Exception ex) {
            throw new RuntimeException("Project cannot be resolved", ex);
        }
    }

}
