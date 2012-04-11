/*
 * Created on Aug 18, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.wizard.base;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.openl.eclipse.wizard.OpenlWizardPlugin;
import org.openl.util.RuntimeExceptionWrapper;
import org.osgi.framework.Bundle;

/**
 * Write once - use everywhere :)
 *
 * @author sam
 */
public class UtilBase {
    /**
     * Default name for the resource bundle.
     */
    static public final String DEFAULT_RESOURCE_BUNDLE_NAME = "Messages";

    /**
     * URL protocol: "file:"
     */
    static public final String FILE_PROTOCOL = "file:";

    /**
     * Used to indicate that required message missed.
     */
    static public final String EMPTY_MESSAGE = "EMPTY_MESSAGE";

    protected String resourceBundleName;

    protected ResourceBundle resourceBundle;

    protected static void append(StringBuffer sb, String s) {
        if (s != null && s.trim().length() > 0) {
            if (sb.length() > 0) {
                sb.append(": ");
            }

            sb.append(s);
        }
    }

    /**
     * Returns new CoreException with a given parameters.
     */
    public static CoreException coreException(String message) {
        return coreException(message, null, -1, IStatus.ERROR);
    }

    /**
     * Returns new CoreException with a given parameters.
     */
    public static CoreException coreException(String message, Throwable t, int code, int severity) {
        if (message == null) {
            message = getMessage(t);
        }
        return new CoreException(new Status(severity, OpenlWizardPlugin.getDefault().getBundle().getSymbolicName(),
                code, message, t));
    }

    /**
     * Returns new CoreException with a given parameters.
     */
    public static CoreException coreException(Throwable t) {
        if (t instanceof CoreException) {
            return (CoreException) t;
        }

        if (t instanceof InvocationTargetException) {
            t = unwrapInvocationTargetException((InvocationTargetException) t);
        }

        return coreException(null, t, -1, IStatus.ERROR);
    }

    static boolean endsWithSlash(String s) {
        return s.length() > 0 && isSlash(s.charAt(s.length() - 1));
    }

    public static IWorkbenchWindow getActiveWorkbenchWindow() {
        return OpenlWizardPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
    }

    // TODO re-think: 'log' plugin
    private static Plugin getLogPlugin() {
        return OpenlWizardPlugin.getDefault();
    }

    // TODO re-think: unwrap exception stack
    public static String getMessage(Throwable t) {
        StringBuffer sb = new StringBuffer();

        append(sb, t.getMessage());

        if (sb.length() == 0) {
            append(sb, t.getClass().getName() + ": NO_MESSAGE");
        }

        return sb.toString();
    }

    /**
     * Generic exception handler.
     */
    public static CoreException handleException(String message) {
        CoreException ce = coreException(message);
        log(ce.getStatus());
        return ce;
    }

    /**
     * Generic exception handler.
     */
    public static CoreException handleException(Throwable t) {
        CoreException ce = coreException(t);
        log(ce.getStatus());
        return ce;
    }

    static boolean isSlash(char c) {
        return c == '/' || c == '\\';
    }

    /**
     * TODO re-think: logging
     */
    public static void log(IStatus status) {
        getLogPlugin().getLog().log(status);

        // if (needToShowEddorDialog(status))
        // showErrorDialog(status);
    }

    /**
     * Returns new CoreException with a given parameters.
     */
    public static Throwable unwrapInvocationTargetException(InvocationTargetException t) {
        Throwable cause;

        while ((cause = t.getCause()) instanceof InvocationTargetException) {
            t = (InvocationTargetException) cause;
        }

        return cause != null ? cause : t;
    }

    public int arrayLength(Object[] ary) {
        return ary == null ? 0 : ary.length;
    }

    public IDecoratorManager getDecoratorManager() {
        return getWorkbench().getDecoratorManager();
    }

    public String getDefaultResourceBundleName() {
        return getClass().getPackage().getName() + '.' + DEFAULT_RESOURCE_BUNDLE_NAME;
    }

    public IEditorRegistry getEditorRegistry() {
        return getWorkbench().getEditorRegistry();
    }

    public String getFormattedString(String key, Object arg) {
        return getFormattedString(key, new Object[] { arg });
    }

    public String getFormattedString(String key, Object[] args) {
        return MessageFormat.format(getString(key), args);
    }

    public ResourceBundle getResourceBundle() {
        if (resourceBundle == null) {
            resourceBundle = loadResourceBundle();
        }
        return resourceBundle;
    }

    public String getResourceBundleName() {
        return resourceBundleName != null ? resourceBundleName : getDefaultResourceBundleName();
    }

    /**
     * Convenience method to get the active window's Shell.
     */
    public Shell getShell() {
        if (getActiveWorkbenchWindow() != null) {
            return getActiveWorkbenchWindow().getShell();
        }
        return null;
    }

    public String getString(String key) {
        return getString(key, key);
    }

    public String getString(String key, String defaultValue) {
        String result = null;

        try {
            result = getResourceBundle().getString(key);
        } catch (Throwable t) {
            handleException(t);
        }

        return !isEmpty(result) ? result : defaultValue;
    }

    public String[] getTokens(String s) {
        return getTokens(s, ", \t\n\r\f");
    }

    public String[] getTokens(String s, String delimiters) {
        StringTokenizer t = new StringTokenizer(s, delimiters);
        Collection<String> result = new ArrayList<String>(t.countTokens());
        while (t.hasMoreTokens()) {
            result.add(t.nextToken());
        }

        return result.toArray(new String[0]);
    }

    public IWorkbench getWorkbench() {
        return PlatformUI.getWorkbench();
    }

    public IWorkspace getWorkspace() {
        return ResourcesPlugin.getWorkspace();
    }

    public IWorkspaceRoot getWorkspaceRoot() {
        return getWorkspace().getRoot();
    }

    /**
     * Guesses if path is File URL.
     *
     * @return canonical URL for the File or null.
     */
    URL guessFileURL(String url) {
        url = ltrimFileURL(url);

        // Wintel's "C:..." are files, other - not (poor Mac:)
        int idx = url.indexOf(':');
        if (idx == 0 || idx > 1) {
            return null;
        }

        try {
            return new File(url).getCanonicalFile().toURL();
        } catch (Exception e) {
            throw handleException1(e);
        }
    }

    /**
     * Generic exception handler.
     */
    // TODO re-think: handleException
    public RuntimeException handleException1(Throwable t) {
        return RuntimeExceptionWrapper.wrap(handleException(t));
    }

    public boolean isEmpty(String s) {
        return s == null || s.length() == 0 || s.trim().length() == 0;
    }

    public ResourceBundle loadResourceBundle() {
        try {
            String bundleName = getResourceBundleName();
            Locale locale = Locale.getDefault();
            ClassLoader cl = getClass().getClassLoader();
            // TODO submit bug report:
            // ResourceBundle.getBundle(bundleName, Locale.getDefault(),
            // getClass().getClassLoader());
            // != ResourceBundle.getBundle(bundleName) ???
            return ResourceBundle.getBundle(bundleName, locale, cl);
        } catch (Throwable t) {
            throw handleException1(t);
            // return null;
        }
    }

    String ltrimFileURL(String url) {
        url = url.replace('\\', '/');

        if (url.startsWith(FILE_PROTOCOL)) {
            url = url.substring(FILE_PROTOCOL.length());
        }

        // Wintel's "/C:..." - just in case...
        if (url.indexOf('/') == 0 && url.indexOf(':') == 2) {
            url = url.substring(1);
        }

        return url;
    }

    /**
     * TODO re-think: needToShowEddorDialog
     */
    public boolean needToShowEddorDialog(IStatus status) {
        int statusMask = IStatus.ERROR;

        return (statusMask & status.getSeverity()) != 0;
    }

    protected CoreException NOT_IMPLEMENTED_METHOD(String methodName) {
        String message = "NOT IMPLEMENTED METHOD: " + getClass().getName() + "." + methodName;
        return handleException(message);
    }

    public void setResourceBundleName(String resourceBundleName) {
        this.resourceBundleName = resourceBundleName;
        resourceBundle = null;
    }

    // ////////////////////////////////////////////////////////////////////
    // Convenience access to singletons
    // ////////////////////////////////////////////////////////////////////

    public void showErrorDialog(final IStatus status) {
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                String dialogTitle = "Exception:";
                ErrorDialog.openError(getShell(), dialogTitle, null, status);
            }
        });
    }

    /**
     * Returns canonical URL for the plugin-relative-path. Returns null if path
     * is not valid or does not exist.
     */
    public String toCanonicalUrl(Bundle pd, String path) {
        try {
            // URL url = Platform.find(pd, new Path(path));
            URL url = FileLocator.find(pd, new Path(path), null);
            // url = Platform.asLocalURL(url);
            url = FileLocator.toFileURL(url);
            if (url == null) {
                return null;
            }
            String s = toCanonicalURL(url.toString());

            // remove ending slash added by conversion
            if (!endsWithSlash(path) && endsWithSlash(s)) {
                s = s.substring(0, s.length() - 1);
            }

            return s;
        } catch (Throwable t) {
            throw handleException1(t);
        }
    }

    public String toCanonicalURL(IPath path) {
        return toCanonicalURL(path.toString());
    }

    /**
     * Returns canonical URL for the path. Note: File URLs are without "file:"
     * protocol.
     */
    public String toCanonicalURL(String path) {
        String url = toURL(path).toExternalForm();

        url = ltrimFileURL(url);

        return url;
    }

    /**
     * Returns canonical URL for the (parent,child)-resource.
     */
    public String toCanonicalURL(String parent, String child) {
        String url = toCanonicalURL(parent);
        if (child != null && child.length() > 0) {
            url += '/' + child;
        }
        return url;
    }

    /**
     * Returns URL for the path.
     */
    public URL toURL(String path) {
        // 'new URL()' is NOGOOD for Wintel's names - guess ourself.
        URL url = guessFileURL(path);
        if (url != null) {
            return url;
        }

        try {
            return new URL(path);
        } catch (Exception e) {
            throw handleException1(e);
        }
    }

    public URL[] toURL(String[] path) {
        URL[] urls = new URL[path.length];

        for (int i = 0; i < path.length; i++) {
            urls[i] = toURL(path[i]);
        }

        return urls;
    }

}