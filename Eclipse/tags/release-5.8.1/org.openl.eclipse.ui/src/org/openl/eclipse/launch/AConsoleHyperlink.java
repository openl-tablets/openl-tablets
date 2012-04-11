/*
 * Created on Dec 18, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.launch;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.openl.eclipse.base.OpenlBasePlugin;
import org.openl.eclipse.util.ResourceUtil;
import org.openl.main.SourceCodeURLConstants;
import org.openl.main.SourceCodeURLTool;

/**
 *
 * @author sam
 */
public abstract class AConsoleHyperlink extends LaunchBase implements IHyperlink, SourceCodeURLConstants {
    protected IConsole console;

    protected String url;

    public AConsoleHyperlink(IConsole console, String url) {
        this.console = console;
        this.url = url;
    }

    /**
     * Helper to activate text editor for this link.
     */
    protected void activateTextEditor(String url) throws Exception {

        Map<String, String> urlMap = SourceCodeURLTool.parseUrl(url);

        String fileName = (String) urlMap.get(FILE);

        IEditorPart sourceEditor = getSourceEditor(fileName);

        if (!(sourceEditor instanceof ITextEditor)) {
            MessageDialog.openInformation(getShell(), getString("Information"), getString("Source_not_found"));

            return;
        }

        ITextEditor textEditor = (ITextEditor) getSourceEditor(fileName);

        int start = atoi((String) urlMap.get(START), 0);
        int end = atoi((String) urlMap.get(END), start);

        textEditor.selectAndReveal(start, end - start + 1);
    }

    /**
     * Returns the console this link is contained in.
     */
    protected IConsole getConsole() {
        return console;
    }

    private String getEditorId(IFile file) {
        IWorkbench workbench = getWorkbench();
        // If there is a registered editor for the file use it.
        IEditorDescriptor desc = workbench.getEditorRegistry().getDefaultEditor(file.getName());
        if (desc == null) {
            // default editor
            desc = workbench.getEditorRegistry().findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
        }
        return desc.getId();
    }

    private IEditorPart getSourceEditor(String fileName) {
        IWorkbenchWindow window = getActiveWorkbenchWindow();

        IResource resource = ResourceUtil.findWorkspaceResource(fileName);

        if (resource == null || !(resource instanceof IFile)) {
            return null;
        }
        IFile file = (IFile) resource;

        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                try {
                    IEditorPart editorPart = page.openEditor(new FileEditorInput(file), getEditorId(file), false);
                    return editorPart;
                } catch (PartInitException e) {
                    getLogPlugin().getLog().log(
                            new Status(IStatus.ERROR, OpenlBasePlugin.PLUGIN_ID, IStatus.OK, e.getMessage(), e));
                }
            }
        }
        return null;
    }

    /**
     * @see org.eclipse.debug.ui.console.IHyperlink#linkEntered()
     */
    public void linkEntered() {
    }

    /**
     * @see org.eclipse.debug.ui.console.IHyperlink#linkExited()
     */
    public void linkExited() {
    }

    // /**
    // * Returns text for this link.
    // *
    // * @exception CoreException if unable to retrieve the text
    // */
    // protected String getLinkText() throws Exception
    // {
    // try
    // {
    // IRegion region = getConsole().getRegion(this);
    // return getConsole().getDocument().get(
    // region.getOffset(),
    // region.getLength());
    // }
    // catch (BadLocationException e)
    // {
    // throw handleException(getString("Unable_to_retrieve_hyperlink_text"));
    // }
    // }

    // TODO to Eclipse: refactored JavaStackTraceHyperlink implementation.
    //
    // /**
    // * Returns the fully qualified name of the type to open
    // *
    // * @return fully qualified type name
    // * @exception CoreException if unable to parse the type name
    // */
    // protected String getTypeName() throws CoreException
    // {
    // String linkText = getLinkText();
    // int index = linkText.lastIndexOf('(');
    // if (index >= 0)
    // {
    // String typeName = linkText.substring(0, index);
    // // remove the method name
    // index = typeName.lastIndexOf('.');
    // if (index >= 0)
    // {
    // typeName = typeName.substring(0, index);
    // }
    // return typeName;
    // }
    //
    // throw handleException("Unable_to_parse_type_name_from_hyperlink");
    // }
    //
    // protected ITextEditor getSourceEditor(IJavaSourceLocation location,
    // String typeName)
    // {
    // try
    // {
    // Object sourceElement = location.findSourceElement(typeName);
    // return sourceElement != null ?
    // getSourceEditorForSourceElement(sourceElement) : null;
    // }
    // catch (Exception e)
    // {
    // handleException(e);
    // return null;
    // }
    // }
    //
    // /**
    // * Returns this link's text
    // *
    // * @exception CoreException if unable to retrieve the text
    // */
    // protected ITextEditor getSourceEditor(IJavaSourceLocation[] locations,
    // String typeName)
    // {
    // ITextEditor editor;
    //
    // for (int i = 0; i < locations.length; i++)
    // {
    // editor = getSourceEditor(locations[i], typeName);
    // if (editor != null)
    // return editor;
    // }
    //
    // return null;
    // }
    //
    // /**
    // * Returns the locations in which to look for source associatd with the
    // * stack trace, or <code>null</code> if none.
    // *
    // * @return IJavaSourceLocation[]
    // */
    // protected IJavaSourceLocation[] getJavaSourceLocations()
    // {
    // ISourceLocator sourceLocator = null;
    //
    // ILaunch launch = getConsole().getProcess().getLaunch();
    // if (launch != null)
    // {
    // sourceLocator = launch.getSourceLocator();
    // }
    //
    // IJavaSourceLocation[] sourceLocations = null;
    // if (sourceLocator instanceof JavaSourceLocator)
    // {
    // sourceLocations =
    // ((JavaSourceLocator)sourceLocator).getSourceLocations();
    // }
    // else if (sourceLocator instanceof JavaUISourceLocator)
    // {
    // sourceLocations =
    // ((JavaUISourceLocator)sourceLocator).getSourceLocations();
    // }
    //
    // if (sourceLocations == null)
    // {
    // // create a source locator using all projects in the workspace
    // IJavaModel javaModel =
    // JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
    // if (javaModel == null)
    // {
    // return null;
    // }
    // try
    // {
    // sourceLocator = new JavaUISourceLocator(javaModel.getJavaProjects(),
    // false);
    // }
    // catch (JavaModelException e)
    // {
    // handleException("Unable_to_retrieve_workspace_source");
    // return null;
    // }
    // sourceLocations =
    // ((JavaUISourceLocator)sourceLocator).getSourceLocations();
    // }
    //
    // return sourceLocations;
    // }
    //
}
