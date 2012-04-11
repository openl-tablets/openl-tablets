/*
 * Created on Dec 11, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.rules.eclipse.xls.editor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.openl.rules.eclipse.xls.launching.ExcelLauncher;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author sam
 */
public class XlsEditor extends MultiPageEditorPart implements IGotoMarker {

    static public String URL = "url";

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.MultiPageEditorPart#createPages()
     */
    @Override
    protected void createPages() {
        try {
            IEditorPart editor = new TempEditor();

            // IEditorInput input = getEditorInput();
            String name = "**name";
            String toolTipText = "" + "To edit this file using Microsoft Excel\n"
                    + "Select 'Open With|System Editor' in right-button menu" + "";
            IEditorInput input = new XlsEditorInput(name, toolTipText);

            addPage(editor, input);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs() {
        // TODO Auto-generated method stub

    }

    String getUrl(IMarker marker) throws Exception {
        String url = (String) marker.getAttribute(URL);

        if (url != null) {
            return url;
        }

        throw new RuntimeException("Marker has no 'URL' attribute: " + marker);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#gotoMarker(org.eclipse.core.resources.IMarker)
     */
    public void gotoMarker(IMarker marker) {
        try {
            ExcelLauncher.launch(getUrl(marker));
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    @Override
    public boolean isSaveAsAllowed() {
        // TODO Auto-generated method stub
        return false;
    }

}