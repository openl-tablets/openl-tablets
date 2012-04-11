package org.openl.eclipse.wizard.base;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.openl.eclipse.wizard.base.internal.OpenLProjectCreator;
import org.openl.eclipse.wizard.base.internal.TemplateCopier;
import org.openl.eclipse.wizard.base.internal.DependenciesManifestParser;
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
        creator.setupClasspath(false, getTemplateSourceDirectories(customizer));

        TemplateCopier templateCopier = new TemplateCopier(project, customizer);

        templateCopier.setIgnoreManifect(true);
        templateCopier.copy(null);
    }

    public static String[] getProjectDependencies(INewProjectFromTemplateWizardCustomizer customizer) {
        String projectLocation = getTemplateLocation(customizer);
        File manifestFile = new File(projectLocation, "META-INF/MANIFEST.MF");
        return new DependenciesManifestParser(manifestFile).getDependencies();
    }

    public static Properties getProjectProperties(INewProjectFromTemplateWizardCustomizer customizer) {
        Properties projectProperties = new Properties();
        try {
            projectProperties.load(new FileInputStream(new File(getTemplateLocation(customizer), ".info")));
        } catch (IOException no_info_file) {
        }

        return projectProperties;
    }

    private static String getTemplateLocation(INewProjectFromTemplateWizardCustomizer customizer) {
        Properties properties = new Properties();
        customizer.setTemplateProperties(properties);
        return properties.getProperty(INewProjectFromTemplateWizardCustomizerConstants.PROP_SRC_DIR);
    }

    public static String[] getTemplateSourceDirectories(INewProjectFromTemplateWizardCustomizer customizer) {
        String s = getProjectProperties(customizer).getProperty("sources");
        if (s == null) {
            return new String[0];
        }
        return s.trim().split(",");
    }

    public static void removeOpenLCapabilities(IProject project) throws CoreException {
        if (!project.hasNature(IOpenlConstants.OPENL_NATURE_ID)) {
            return;
        }

        new OpenLProjectCreator(project, project.getLocation()).removeProjectNature(IOpenlConstants.OPENL_NATURE_ID);
    }

}
