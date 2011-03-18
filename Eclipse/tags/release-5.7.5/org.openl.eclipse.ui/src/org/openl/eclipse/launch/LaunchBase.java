/*
 * Created on Aug 12, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.openl.eclipse.util.UtilBase;
import org.openl.util.ASelector;
import org.openl.util.IOpenIterator;
import org.openl.util.ISelector;
import org.openl.util.OpenIterator;

/**
 * The implementation of the ILaunchBase.
 *
 * @author sam
 */
public class LaunchBase extends UtilBase implements ILaunchBase {
    public IOpenIterator getLaunchConfigurations() throws CoreException {
        return OpenIterator.fromArray(getLaunchManager().getLaunchConfigurations());
    }

    public ILaunchManager getLaunchManager() {
        return DebugPlugin.getDefault().getLaunchManager();
    }

    public ISelector launchConfigurationSupportsMode(final String mode) {
        return new ASelector() {

            public boolean select(Object o) {
                try {
                    return ((ILaunchConfiguration) o).supportsMode(mode);
                } catch (Exception e) {
                    handleException(e);
                    return false;
                }
            }
        };
    }

    // public LaunchConfigurationManager getLaunchConfigurationManager()
    // {
    // return DebugUIPlugin.getDefault().getLaunchConfigurationManager();
    // }

    public String MSG_NO_CONFIGURATIONS_TO_LAUNCH(ILaunchRequest selection) {
        return "No_configurations_to_" + selection.getLaunchMode();
    }

    public String MSG_NO_TARGETS_TO_LAUNCH(ILaunchRequest selection) {
        return "No_targets_in_" + (selection.isFromEditor() ? "active_editor" : "selection") + "_to_"
                + selection.getLaunchMode();
    }

    /**
     * Shows a selection dialog. Returns chosen element(s) or null if dialog was
     * cancelled.
     */
    public Object[] selectionDialog(Object[] elements, String title, String message, boolean multi) {
        // TODO re-think: implement DebugModelPresentation extension for
        // ILaunchTarget?
        ILabelProvider labelProvider = DebugUITools.newDebugModelPresentation();
        try {
            ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), labelProvider);

            dialog.setTitle(title);
            dialog.setMessage(message);
            dialog.setElements(elements);
            dialog.setMultipleSelection(multi);

            dialog.open();

            return dialog.getResult();
        } finally {
            labelProvider.dispose();
        }
    }

    public String uniqueLCName(String prefix) {
        return getLaunchManager().generateUniqueLaunchConfigurationNameFrom(prefix);
    }

}
