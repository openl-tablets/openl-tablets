/*
 * Created on Aug 11, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.openl.eclipse.util.Debug;
import org.openl.eclipse.util.XxY;
import org.openl.util.ASelector;
import org.openl.util.IConvertor;
import org.openl.util.ISelector;

/**
 * TODO re-think: use ILaunchTarget instead of Object as target. TODO re-think:
 * is IResource suitable for all cases? How about IStorage? Generic
 * implementation of the ILaunchShortcut: - Find targets that can be launched
 * within selection - Find configurations or create new that launch found
 * targets - Launch above configurations
 *
 * @author sam
 */
public abstract class ALaunchShortcut extends LaunchBase implements ILaunchShortcut {
    /**
     * Returns collector: object to launchTarget. For example, IResource to
     * OpenlLaunchTarget.
     */
    abstract public IConvertor asLaunchTarget();

    /**
     * Dialog to choose one of the given launch configurations. Returns chosen
     * configuration or <code>null</code> if user cancelled the dialog.
     */
    public ILaunchConfiguration[] chooseLaunchConfigurations(ILaunchRequest request,
            ILaunchConfiguration[] launchConfigurations, boolean multipleSelection) {
        return (ILaunchConfiguration[]) selectionDialog(launchConfigurations,
                getString(MSG_LAUNCH_CONFIG_SELECTION_TITLE), getFormattedString(MSG_LAUNCH_CONFIG_SELECTION_MESSAGE,
                        request.getLaunchMode()), multipleSelection);
    }

    /**
     * Shows a selection dialog for launch targets. Returns chosen targets or
     * <code>null</code> if user cancelled the dialog.
     */
    public ILaunchTarget[] chooseLaunchTargets(ILaunchRequest request, ILaunchTarget[] launchTargets,
            boolean multipleSelection) {
        Object[] res = selectionDialog(launchTargets, getString(MSG_LAUNCH_TARGET_SELECTION_TITLE), getFormattedString(
                MSG_LAUNCH_TARGET_SELECTION_MESSAGE, request.getLaunchMode()), multipleSelection);

        ILaunchTarget[] resL = new ILaunchTarget[res.length];
        for (int i = 0; i < res.length; i++) {
            resL[i] = (ILaunchTarget) res[i];
        }
        return resL;
    }

    /**
     * Returns launch request for a given editor.
     */
    public ILaunchRequest createLaunchRequest(IEditorPart editor, String mode) {
        return new LaunchRequest(editor, mode);
    }

    /**
     * Returns launch request for a given selection.
     */
    public ILaunchRequest createLaunchRequest(ISelection selection, String mode) {
        return selection instanceof IStructuredSelection ? new LaunchRequest((IStructuredSelection) selection, mode)
                : null;
    }

    /**
     * Returns existing configurations that launch given target.
     */
    protected ILaunchConfiguration[] getExistingLaunchConfigurations(ILaunchRequest request, ILaunchTarget[] targets) {
        try {
            return (ILaunchConfiguration[]) getLaunchConfigurations().select(
                    launchConfigurationForTargets(targets)
                            .and(launchConfigurationSupportsMode(request.getLaunchMode()))).asList().toArray(
                    NO_CONFIGURATIONS);
        } catch (CoreException e) {
            handleException(e);
            return NO_CONFIGURATIONS;
        }
    }

    ILaunchGroup getLaunchConfigurationGroup(ILaunchConfiguration configuration, ILaunchRequest request) {

        ILaunchGroup group = DebugUITools.getLaunchGroup(configuration, request.getLaunchMode());

        // if (group == null)
        // {
        // group =
        // getLaunchConfigurationManager().getDefaultLanuchGroup(
        // request.getLaunchMode());
        // }

        if (group == null) {
            throw new InternalError("LaunchConfigurationManager.getDefaultLanuchGroup() returns null");
        }

        return group;
    }

    /**
     * Returns configurations that launch given targets. Creates default
     * configuration when target has no configuration.
     */
    protected ILaunchConfiguration[] getLaunchConfigurations(ILaunchRequest request, ILaunchTarget[] targets) {
        ILaunchConfiguration[] configurations = getExistingLaunchConfigurations(request, targets);

        XxY tc = new XxY();
        for (int i = 0; i < targets.length; i++) {
            for (int j = 0; j < configurations.length; j++) {
                if (targets[i].isLaunchedBy(configurations[j])) {
                    tc.add(targets[i], configurations[j]);
                }
            }
        }

        // there is no ambiquity -> create defaults for missing configurations
        if (tc.x_Y().isF()) {
            for (int i = 0; i < targets.length; i++) {
                if (!tc.x_Y().X().contains(targets[i])) {
                    ILaunchConfiguration configuration = targets[i].createDefaultLaunchConfiguration(request);

                    // cancelled by user or error
                    if (configuration == null) {
                        return null;
                    }

                    tc.add(targets[i], configuration);
                }
            }

            configurations = (ILaunchConfiguration[]) tc.y_X().X().toArray(NO_CONFIGURATIONS);
        } else {
            configurations = chooseLaunchConfigurations(request, configurations, false);
        }

        if (configurations != null && configurations.length == 0) {
            MessageDialog.openInformation(getShell(), getString(MSG_LAUNCH_FAILED),
                    getString(MSG_NO_CONFIGURATIONS_TO_LAUNCH(request)));
        }

        return configurations != null ? configurations : NO_CONFIGURATIONS;
    }

    /**
     * Returns selected launch targets for the request.
     */
    public ILaunchTarget[] getLaunchTargets(ILaunchRequest request) {
        ILaunchTarget[] targets = (ILaunchTarget[]) request.getSelection().select(selectLaunchTargetType()).collect(
                asLaunchTarget()).select(NOT_NULLS).asList().toArray(NO_TARGETS);

        if (targets.length == 0) {
            MessageDialog.openInformation(getShell(), getString(MSG_LAUNCH_FAILED),
                    getString(MSG_NO_TARGETS_TO_LAUNCH(request)));
        } else if (targets.length > 1) {
            targets = chooseLaunchTargets(request, targets, false);
        }

        return targets != null ? targets : NO_TARGETS;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.ui.IEditorPart,
     *      java.lang.String)
     */
    public void launch(IEditorPart activeEditor, String mode) {
        launch(createLaunchRequest(activeEditor, mode));
    }

    boolean launch(ILaunchConfiguration configuration, ILaunchRequest request, boolean preview) {
        if (preview && !previewConfiguration(configuration, request)) {
            return false;
        }

        if (Debug.DEBUG) {
            Debug.debug("Launching: " + configuration.getName());
        }

        DebugUITools.launch(configuration, request.getLaunchMode());

        return true;
    }

    /**
     * Main launching method.
     */
    public void launch(ILaunchRequest request) {
        ILaunchTarget[] targets = NO_TARGETS;
        ILaunchConfiguration[] configurations = NO_CONFIGURATIONS;

        boolean ok = DebugUITools.saveAndBuildBeforeLaunch() && (targets = getLaunchTargets(request)).length > 0
                && (configurations = getLaunchConfigurations(request, targets)).length > 0;

        if (!ok) {
            return;
        }

        for (int i = 0; i < configurations.length; i++) {
            boolean preview = true;
            if (!launch(configurations[i], request, preview)) {
                return;
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.jface.viewers.ISelection,
     *      java.lang.String)
     */
    public void launch(ISelection selection, String mode) {
        launch(createLaunchRequest(selection, mode));
    }

    /**
     * Returns selector "target is launched by the configuration in question".
     */
    public ISelector launchConfigurationForTarget(final ILaunchTarget target) {
        return new ASelector() {
            public boolean select(Object o) {
                return o instanceof ILaunchConfiguration && target.isLaunchedBy((ILaunchConfiguration) o);
            }
        };
    }

    /**
     * Returns selector "one of the targets is launched by the configuration in
     * question".
     */
    public ISelector launchConfigurationForTargets(ILaunchTarget[] targets) {
        ISelector result = FALSE_SELECTOR;

        for (int i = 0; i < targets.length; i++) {
            result = result.or(launchConfigurationForTarget(targets[i]));
        }

        return result;
    }

    boolean previewConfiguration(ILaunchConfiguration configuration, ILaunchRequest request) {
        String groupId = getLaunchConfigurationGroup(configuration, request).getIdentifier();

        return DebugUITools.openLaunchConfigurationPropertiesDialog(getShell(), configuration, groupId) == Window.OK;
    }

    /**
     * Returns selector: <code>launch-target-is-of-my-type</code>. For
     * example, java compilation unit with main(String[]) method.
     */
    abstract public ISelector selectLaunchTargetType();

}
