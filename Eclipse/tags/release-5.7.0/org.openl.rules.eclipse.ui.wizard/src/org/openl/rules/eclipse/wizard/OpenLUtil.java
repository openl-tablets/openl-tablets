package org.openl.rules.eclipse.wizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizardCustomizer;
import org.openl.eclipse.wizard.base.OpenLCore;

/**
 * @author Aliaksandr Antonik.
 */
public class OpenLUtil {
    public static void addOpenLCapabilities(IProject project) throws CoreException {
        OpenLCore.addOpenLCapabilities(project, getCustomizer());
    }

    private static NewProjectFromTemplateWizardCustomizer getCustomizer() {
        return new NewProjectFromTemplateWizardCustomizer(RulesWizardPlugin.getDefault().getBundle(),
                "NewSimpleOpenLRulesProjectWizard") {
        };
    }

    public static String[] getOpenLProjectDependencies() {
        return OpenLCore.getProjectDependencies(getCustomizer());
    }

    public static String[] getOpenLSourceDirectories() {
        return OpenLCore.getTemplateSourceDirectories(getCustomizer());
    }

    public static void removeOpenLCapabilities(IProject project) throws CoreException {
        OpenLCore.removeOpenLCapabilities(project);
    }
}
