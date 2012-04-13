package org.openl.rules.liveexcel.eclipse.ui.popup.actions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.PluginAction;

@SuppressWarnings("restriction")
public class LiveExcelManifestExtender implements IObjectActionDelegate {

    private Shell shell;

    /**
     * Constructor for Action1.
     */
    public LiveExcelManifestExtender() {
        super();
    }

    /**
     * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
     */
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        shell = targetPart.getSite().getShell();
    }

    /**
     * @see IActionDelegate#run(IAction)
     */
    public void run(IAction action) {
        try {
            StructuredSelection selection = (StructuredSelection) ((PluginAction) action).getSelection();
            IJavaProject project = (IJavaProject) selection.getFirstElement();
            IFile manifestFile = project.getProject().getFile(JarFile.MANIFEST_NAME);
            Manifest parsedManifest = null;
            parsedManifest = new Manifest(manifestFile.getContents());
            addLEExtendionDependency(manifestFile, parsedManifest);
            project.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (Exception e) {
            MessageDialog.openInformation(shell, "LiveExcel", "Failed to update project. Reason :" + e.getMessage());
        }
    }

    private void addLEExtendionDependency(IFile manifest, Manifest mf) {
        Attributes.Name dependenciesProp = new Attributes.Name("Require-Bundle");
        String dependeniesValue = (String) mf.getMainAttributes().get(dependenciesProp);
        if (dependeniesValue.contains("org.openl.rules.liveexcel.extension")) {
            MessageDialog.openInformation(shell, "LiveExcel", "This project is already LE");
        } else {
            mf.getMainAttributes().put(dependenciesProp, dependeniesValue + ",org.openl.rules.liveexcel.extension");
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(manifest.getLocation().toFile());
                mf.write(out);
                MessageDialog.openInformation(shell, "LiveExcel", "This project was succesfully updated.");
            } catch (IOException e) {
                MessageDialog
                        .openInformation(shell, "LiveExcel", "Failed to update project. Reason :" + e.getMessage());
            } finally {
                IOUtils.closeQuietly(out);
            }
        }
    }

    /**
     * @see IActionDelegate#selectionChanged(IAction, ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
    }

}
