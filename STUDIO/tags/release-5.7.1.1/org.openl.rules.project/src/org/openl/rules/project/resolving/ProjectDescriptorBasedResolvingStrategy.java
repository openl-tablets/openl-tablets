package org.openl.rules.project.resolving;

import java.io.File;

import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.model.ProjectDescriptor;

public class ProjectDescriptorBasedResolvingStrategy implements ResolvingStrategy {

    public final static String PROJECT_DESCRIPTOR_FILE_NAME = "rules.xml";

    public boolean isRulesProject(File folder) {

        File descriptorFile = new File(folder, PROJECT_DESCRIPTOR_FILE_NAME);
        return descriptorFile.exists();
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
