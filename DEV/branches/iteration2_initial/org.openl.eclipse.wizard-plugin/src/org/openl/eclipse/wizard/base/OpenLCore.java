package org.openl.eclipse.wizard.base;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.openl.eclipse.wizard.base.internal.OpenLProjectCreator;
import org.openl.eclipse.wizard.base.internal.TemplateCopier;
import org.openl.eclipse.util.IOpenlConstants;

/**
 * @author Aliaksandr Antonik.
 */
public class OpenLCore {
    public static void addOpenLCapabilities(IProject project, INewProjectFromTemplateWizardCustomizer customizer)
            throws CoreException {
        if (project.hasNature(IOpenlConstants.OPENL_NATURE_ID)) {
            return;
        }

        OpenLProjectCreator creator = new OpenLProjectCreator(project, project.getLocation());
        creator.addProjectNature(IOpenlConstants.OPENL_NATURE_ID);
        creator.setupClasspath(false);

        TemplateCopier templateCopier = new TemplateCopier(project, customizer);
        templateCopier.setIgnoreManifect(true);
        templateCopier.copy(null);
    }

    public static void removeOpenLCapabilities(IProject project) throws CoreException {
        if (!project.hasNature(IOpenlConstants.OPENL_NATURE_ID)) {
            return;
        }

        new OpenLProjectCreator(project, project.getLocation()).removeProjectNature(IOpenlConstants.OPENL_NATURE_ID);
    }
}
