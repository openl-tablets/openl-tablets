/*
 * Created on 28.10.2004
 */
package org.openl.eclipse.wizard.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

/**
 * @author smesh
 */
public class NewProjectFromTemplateWizard
	extends BasicNewResourceWizard
	implements INewWizard, INewProjectFromTemplateWizardCustomizerConstants {
	private INewProjectFromTemplateWizardCustomizer customizer;

	private WizardNewProjectCreationPage mainPage;

	public NewProjectFromTemplateWizard(INewProjectFromTemplateWizardCustomizer customizer) {
		this.customizer = customizer;
	}

	/**
	 * @see Wizard#init
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		super.init(workbench, selection);
		setNeedsProgressMonitor(true);
		setWindowTitle(customizer.getString(KEY_NEWPROJECT_WINDOW_TITLE));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	public void addPages() {
		try {
			super.addPages();

			mainPage = new WizardNewProjectCreationPage("basicNewProjectPage");
			mainPage.setTitle(customizer.getString(KEY_NEWPROJECT_TITLE));
			mainPage.setDescription(
				customizer.getString(KEY_NEWPROJECT_DESCRIPTION));

			String initialProjectName =
				customizer.getString(KEY_INITIAL_PROJECT_NAME, null);
			if (initialProjectName != null)
				mainPage.setInitialProjectName(initialProjectName);

			this.addPage(mainPage);

		} catch (Throwable t) {
			customizer.handleException(t);
		}

	}

	public boolean performFinish() {
		IProject newProject = createNewProject();

		if (newProject == null)
			return false;

		//    updatePerspective();
		selectAndReveal(newProject);

		return true;
	}

	private IProject createNewProject() {
		// get a project handle
		final IProject newProjectHandle = mainPage.getProjectHandle();

		// get a project descriptor
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProjectDescription description =
			workspace.newProjectDescription(newProjectHandle.getName());

		if (!mainPage.useDefaults()) {
			description.setLocation(mainPage.getLocationPath());
		}

		// create the new project operation
		WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			protected void execute(IProgressMonitor monitor)
				throws CoreException {
				createProject(description, newProjectHandle, monitor);
				// run ant build
				{
					Properties properties = new Properties();
					customizer.setAntBuildFileProperties(properties);
					String dstDir = newProjectHandle.getLocation().toOSString();
					String dstProjectName = newProjectHandle.getName();
					properties.setProperty(PROP_DST_DIR, dstDir);
					properties.setProperty(PROP_DST_PROJECT_NAME, dstProjectName);

					properties.setProperty(PROP_GEN_DIR, PROP_GEN_DIR_VALUE);
					properties.setProperty(PROP_JAVA_PKG, PROP_JAVA_PKG_VALUE);
					
					
					runAnt(
						customizer.getAntBuildFileLocation(),
						properties,
						monitor);
				}
				// refresh workspace project
				newProjectHandle.refreshLocal(
					IResource.DEPTH_INFINITE,
					monitor);
			}
		};

		try {
			getContainer().run(true, true, op);
		} catch (Exception e) {
			customizer.handleException(e);
			return null;
		}

		return newProjectHandle;
	}

	private void createProject(
		IProjectDescription description,
		IProject projectHandle,
		IProgressMonitor monitor)
		throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask("", 2000);

			projectHandle.create(
				description,
				new SubProgressMonitor(monitor, 1000));

			if (monitor.isCanceled())
				throw new OperationCanceledException();

			projectHandle.open(new SubProgressMonitor(monitor, 1000));

		} finally {
			monitor.done();
		}
	}

	private void runAnt(
		String buildFileLocation,
		Properties properties,
		IProgressMonitor monitor)
		throws CoreException {
		try {
			monitor.beginTask("", 2000);

			AntRunner runner = new AntRunner();

			runner.setBuildFileLocation(customizer.getAntBuildFileLocation());

			// set arguments
			Collection args = new ArrayList();
			for (Iterator it = properties.entrySet().iterator();
				it.hasNext();
				) {
				Map.Entry entry = (Map.Entry) it.next();
				args.add("-D" + entry.getKey() + "=" + entry.getValue());
			}
			args.add("-verbose");

			runner.setArguments(
				(String[]) args.toArray(new String[args.size()]));

			runner.run(monitor);

			// wait
			while (AntRunner.isBuildRunning()) {
				wait(1000);
				monitor.worked(1);
			}
		} catch (Throwable e) {
			throw customizer.handleException(e);
		} finally {
			monitor.done();
		}
	}

}
