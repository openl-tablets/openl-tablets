/*
 * Created on Jul 10, 2003
 */
package org.openl.eclipse.base;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.openl.eclipse.util.IOpenlConstants;

/**
 * @author sam
 */
public class OpenlNature implements IProjectNature {
    IProject openLProject = null;

    /**
     * Adds a builder to the build spec for the given project if it is not
     * already there.
     *
     * @param builderID builder id
     * @throws org.eclipse.core.runtime.CoreException on error
     */
    private void addToBuildSpec(String builderID) throws CoreException {
        IProjectDescription description = openLProject.getDescription();
        for (ICommand c : description.getBuildSpec()) {
            if (c.getBuilderName().equals(builderID)) {
                return;
            }
        }

        // Add a Java command to the build spec
        ICommand command = description.newCommand();
        command.setBuilderName(builderID);
        setJavaCommand(description, command);
    }

    public void configure() throws CoreException {
        addToBuildSpec(IOpenlConstants.OPENL_BUILDER_NAME);
    }

    public void deconfigure() throws CoreException {
        removeFromBuildSpec(IOpenlConstants.OPENL_BUILDER_NAME);
    }

    public IProject getProject() {
        return openLProject;
    }

    /**
     * Removes the given builder from the build spec for the given project.
     *
     * @param builderID build id to remove
     * @throws org.eclipse.core.runtime.CoreException on error
     */
    protected void removeFromBuildSpec(String builderID) throws CoreException {
        IProjectDescription description = openLProject.getDescription();
        ICommand[] commands = description.getBuildSpec();

        for (int i = 0; i < commands.length; ++i) {
            if (commands[i].getBuilderName().equals(builderID)) {
                ICommand[] newCommands = new ICommand[commands.length - 1];
                System.arraycopy(commands, 0, newCommands, 0, i);
                System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
                description.setBuildSpec(newCommands);
                openLProject.setDescription(description, null);
                return;
            }
        }
    }

    /**
     * Adds the Java command in the build spec.
     *
     * @param description project description
     * @param newCommand command to add
     * @throws org.eclipse.core.runtime.CoreException on error
     */
    private void setJavaCommand(IProjectDescription description, ICommand newCommand) throws CoreException {
        ICommand[] oldBuildSpec = description.getBuildSpec();
        ICommand[] newCommands = new ICommand[oldBuildSpec.length + 1];
        System.arraycopy(oldBuildSpec, 0, newCommands, 0, oldBuildSpec.length);
        newCommands[oldBuildSpec.length] = newCommand;

        // Commit the spec change into the project
        description.setBuildSpec(newCommands);
        openLProject.setDescription(description, null);
    }

    public void setProject(IProject project) {
        openLProject = project;
    }
}