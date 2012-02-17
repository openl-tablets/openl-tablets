/*
 * Created on 28.10.2004
 */
package org.openl.eclipse.wizard.base;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.openl.eclipse.util.IOpenlConstants;
import org.openl.eclipse.wizard.base.internal.OpenLProjectCreator;
import org.openl.eclipse.wizard.base.internal.TemplateCopier;

/**
 * @author smesh
 */
public class NewProjectFromTemplateWizard extends BasicNewResourceWizard implements INewWizard,
        INewProjectFromTemplateWizardCustomizerConstants {
    private INewProjectFromTemplateWizardCustomizer customizer;

    private WizardNewProjectCreationPage mainPage;

    public NewProjectFromTemplateWizard(INewProjectFromTemplateWizardCustomizer customizer) {
        this.customizer = customizer;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    @Override
    public void addPages() {
        try {
            super.addPages();

            mainPage = new WizardNewProjectCreationPage("basicNewProjectPage");
            mainPage.setTitle(customizer.getString(KEY_NEWPROJECT_TITLE));
            mainPage.setDescription(customizer.getString(KEY_NEWPROJECT_DESCRIPTION));

            String initialProjectName = customizer.getString(KEY_INITIAL_PROJECT_NAME, null);
            if (initialProjectName != null) {
                mainPage.setInitialProjectName(initialProjectName);
            }

            addPage(mainPage);

        } catch (Throwable t) {
            UtilBase.handleException(t);
        }

    }

    private IProject createNewProject() {
        final OpenLProjectCreator creator = new OpenLProjectCreator(mainPage.getProjectHandle(),
                mainPage.useDefaults() ? null : mainPage.getLocationPath());

        // create the new project operation
        WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
            @Override
            protected void execute(IProgressMonitor monitor) throws CoreException {
                try {
                    monitor.beginTask(null, 2000);

                    creator.createAndOpen(new SubProgressMonitor(monitor, 800));
                    if (monitor.isCanceled()) {
                        throw new OperationCanceledException();
                    }
                    // run ant build
                    {
                        TemplateCopier copier = new TemplateCopier(creator.getProject(), customizer);
                        copier.copy(new SubProgressMonitor(monitor, 800));
                    }

                    final String[] natures = new String[] {
                    // IOpenlConstants.OPENL_NATURE_ID, JavaCore.NATURE_ID,
                    // PDE.PLUGIN_NATURE
                            IOpenlConstants.OPENL_NATURE_ID, JavaCore.NATURE_ID, "org.eclipse.pde.PluginNature" };

                    for (String nature : natures) {
                        creator.addProjectNature(nature);
                    }

                    monitor.worked(100);

                    creator.setupClasspath(true, OpenLCore.getTemplateSourceDirectories(customizer),
                            OpenLCore.getTemplateLibraries(customizer));
                    monitor.worked(300);

                    // refresh workspace project
                    creator.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);

                } finally {
                    monitor.done();
                }
            }
        };

        try {
            getContainer().run(true, true, op);
        } catch (Exception e) {
            UtilBase.handleException(e);
            return null;
        }

        return creator.getProject();
    }

    /**
     * @see org.eclipse.ui.wizards.newresource.BasicNewResourceWizard#init
     */
    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection);
        setNeedsProgressMonitor(true);
        setWindowTitle(customizer.getString(KEY_NEWPROJECT_WINDOW_TITLE));
    }

    @Override
    public boolean performFinish() {
        IProject newProject = createNewProject();

        if (newProject == null) {
            return false;
        }

        // updatePerspective();
        selectAndReveal(newProject);

        return true;
    }
}
