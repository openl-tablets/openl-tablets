package org.openl.eclipse.util;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.openl.eclipse.base.OpenlBasePlugin;
import org.openl.eclipse.builder.OpenlBuilder;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.util.ASelector;
import org.openl.util.ISelector;
import org.openl.util.RuntimeExceptionWrapper;

public class UtilBase implements IUtilBase {

    protected String resourceBundleName;

    protected ResourceBundle resourceBundle;

    static private void appendErrorMessage(StringBuffer sb, String s) {
        if (s != null && s.trim().length() > 0) {
            if (sb.length() > 0) {
                sb.append(": ");
            }

            sb.append(s);
        }
    }

    static public int atoi(String a, int defaultValue) {
        try {
            return Integer.parseInt(a);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static IWorkbenchWindow getActiveWorkbenchWindow() {
        return OpenlBasePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
    }

    // TODO re-think: 'log' plugin
    static public Plugin getLogPlugin() {
        return OpenlBasePlugin.getDefault();
    }

    protected void append(StringBuffer sb, String s) {
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
    public CoreException coreException(String message) {
        return coreException(message, null, -1, IStatus.ERROR);
    }

    /**
     * Returns new CoreException with a given parameters.
     */
    public CoreException coreException(String message, Throwable t, int code, int severity) {
        return new CoreException(new Status(severity, OpenlBasePlugin.getDefault().getBundle().getSymbolicName(), code,
                getMessage(t), t));
    }

    /**
     * Returns new CoreException with a given parameters.
     */
    public CoreException coreException(Throwable t) {
        if (t instanceof CoreException) {
            return (CoreException) t;
        }

        if (t instanceof InvocationTargetException) {
            t = unwrapInvocationTargetException((InvocationTargetException) t);
        }

        return coreException(null, t, -1, IStatus.ERROR);
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

    public String getMessage(SyntaxNodeException error) {
        StringBuffer sb = new StringBuffer();

        appendErrorMessage(sb, error.getMessage());

        appendErrorMessage(sb, getMessage(error.getOriginalCause()));

        return sb.toString();
    }

    // TODO re-think: unwrap exception stack
    public String getMessage(Throwable t) {
        StringBuffer sb = new StringBuffer();

        append(sb, t.getMessage());

        if (sb.length() == 0) {
            append(sb, t.getClass().getName() + ": NO_MESSAGE");
        }

        return sb.toString();
    }

    public IResource getResourceAdapter(IAdaptable adaptable) {
        if (adaptable instanceof IJavaElement) {
            try {
                return ((IJavaElement) adaptable).getCorrespondingResource();
            } catch (Exception e) {
                handleException(e);
                return null;
            }
        }

        // IStorageEditorInput
        if (adaptable instanceof IStorageEditorInput) {
            try {
                return getResourceAdapter(((IStorageEditorInput) adaptable).getStorage());
            } catch (Exception e) {
                handleException(e);
                return null;
            }
        }

        // IStorage
        if (adaptable instanceof IStorage) {
            return ResourceUtil.findWorkspaceResource(((IStorage) adaptable).getFullPath());
        }

        // IEditorPart
        if (adaptable instanceof IEditorPart) {
            return getResourceAdapter(((IEditorPart) adaptable).getEditorInput());
        }

        // IFile for IAdaptable
        Object adapter = adaptable.getAdapter(IFile.class);
        // IResource for IAdaptable
        if (adapter == null) {
            adapter = adaptable.getAdapter(IResource.class);
        }

        if (!(adapter instanceof IResource)) {
            return null;
        }

        IResource res = (IResource) adapter;

        return res;
    }

    /**
     * Returns resource associated with this something. For example: - IFile for
     * editor Example, most of IJavaElement's.
     */
    public IResource getResourceAdapter(Object something) {
        if (something instanceof IResource) {
            return (IResource) something;
        }

        if (something instanceof IAdaptable) {
            return getResourceAdapter((IAdaptable) something);
        }

        return null;
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

        return result != null ? result : defaultValue;
    }

    public IWorkbench getWorkbench() {
        return PlatformUI.getWorkbench();
    }

    public CoreException handleException(String message) {
        CoreException ce = coreException(message);
        log(ce.getStatus());
        return ce;
    }

    /**
     * Generic exception handler.
     */
    public CoreException handleException(Throwable t) {
        CoreException ce = coreException(t);
        log(ce.getStatus());
        return ce;
    }

    /**
     * Generic exception handler.
     */
    // TODO re-think: handleException
    public RuntimeException handleException1(Throwable t) {
        return RuntimeExceptionWrapper.wrap(handleException(t));
    }

    public boolean isParentChild(IResource parent, IResource child) {
        for (IResource res = child; res != null; res = res.getParent()) {
            if (res.equals(parent)) {
                return true;
            }
        }
        return false;
    }

    public boolean isParentChild(IResource[] parents, IResource child) {
        for (int i = 0; i < parents.length; i++) {
            if (isParentChild(parents[i], child)) {
                return true;
            }
        }
        return false;
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

    /**
     * TODO re-think: logging
     */
    public void log(IStatus status) {
        getLogPlugin().getLog().log(status);

        // if (needToShowEddorDialog(status))
        // showErrorDialog(status);
    }

    public ISelector selectByPath() {
        return new ASelector() {
            public boolean select(Object o) {
                IResource resource = getResourceAdapter(o);
                return resource != null && resource instanceof IFile && OpenlBuilder.isOnSourcePath((IFile) resource);
            }
        };
    }

    public ISelector selectByPathOld(final IResource[] roots) {
        return new ASelector() {
            public boolean select(Object o) {
                IResource resource = getResourceAdapter(o);
                return resource == null ? false : isParentChild(roots, resource);
            }
        };
    }

    /**
     * Select resource if it is in the sourcepath of its own project.
     */
    public ISelector selectBySourcePath() {
        return new ASelector() {
            Map selectorCache = new HashMap();

            ISelector getSelector(IResource resource) {
                IProject project = resource.getProject();
                ISelector selector = (ISelector) selectorCache.get(project);
                if (selector == null) {
                    selector = selectBySourcePath(project);
                    selectorCache.put(project, selector);
                }
                return selector;
            }

            public boolean select(Object o) {
                IResource resource = getResourceAdapter(o);
                return resource == null ? false : getSelector(resource).select(resource);
            }
        };
    }

    /**
     * Select resource if it is in the sourcepath of the project.
     */
    public ISelector selectBySourcePath(IProject project) {
        return selectByPath();
    }

    public ISelector selectIfProject() {
        return new ASelector() {
            public boolean select(Object o) {
                IResource resource = getResourceAdapter(o);
                return resource == null ? false : resource instanceof IProject;
            }
        };

    }

    public ISelector selectOpenlSource() {
        // return selectBySourcePath().and(
        // selectByOpenlConfiguration().or(selectByOpenlEditor()));
        return selectBySourcePath();
    }

    public void setResourceBundleName(String resourceBundleName) {
        this.resourceBundleName = resourceBundleName;
        resourceBundle = null;
    }

    /**
     * Returns new CoreException with a given parameters.
     */
    public Throwable unwrapInvocationTargetException(InvocationTargetException t) {
        Throwable cause;

        while ((cause = t.getCause()) instanceof InvocationTargetException) {
            t = (InvocationTargetException) cause;
        }

        return cause != null ? cause : t;
    }

}
