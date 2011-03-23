/*
 * Created on Jan 8, 2004
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.eclipse.builder;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.openl.eclipse.base.IOpenlModelConstants;
import org.openl.eclipse.base.OpenlBasePlugin;
import org.openl.main.SourceCodeURLConstants;
import org.openl.main.SourceCodeURLTool;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.util.Log;
import org.openl.util.StringTool;
import org.openl.util.text.ILocation;

/**
 * @author sam
 */
public class OpenlMarkers implements SourceCodeURLConstants, IOpenlModelConstants {

    public static void addMarker(IResource resource, ILocation location, IOpenSourceCodeModule module, String openl,
            String message, int severity) {
        String url = SourceCodeURLTool.makeSourceLocationURL(location, module, openl);

        Map<String, String> urlMap = SourceCodeURLTool.parseUrl(url);

        // !!! String fileName = (String) urlMap.get(FILE);
        // IResource resource = ResourceUtil.findWorkspaceResource(fileName);
        //
        // if (resource == null) {
        // handleException(new Exception("Resource not found: " + fileName));
        // }

        String start = (String) urlMap.get(START);
        String end = (String) urlMap.get(END);
        String line = (String) urlMap.get(LINE);

        addMarker(resource, url, message, severity, start, end, line);
    }

    public static void addMarker(IResource resource, SyntaxNodeException error, String openl, int severity) {
        Throwable t = error.getOriginalCause();

        String message;

        if (t != null) {
            if (t instanceof CompositeSyntaxNodeException) {
                addMarkers(resource, (CompositeSyntaxNodeException) t, openl);
                return;
            }

            message = error.getMessage();
        } else {
            message = error.getMessage();
        }

        addMarker(resource, error.getLocation(), error.getSourceModule(), openl, message, severity);
    }

    public static void addMarker(IResource resource, String url, String message, int severity, String start,
            String end, String line) {
        try {
            Map<String, Object> attributes = new HashMap<String, Object>();

            if (url != null) {
                attributes.put(URL, url);
            }

            attributes.put(IMarker.MESSAGE, toSingleLine(message));

            attributes.put(IMarker.SEVERITY, new Integer(severity));

            if (start != null) {
                attributes.put(IMarker.CHAR_START, new Integer(atoi(start, 0)));
            }

            if (end != null) {
                attributes.put(IMarker.CHAR_END, new Integer(1 + atoi(end, 0)));
            }

            if (line != null) {
                attributes.put(IMarker.LINE_NUMBER, new Integer(atoi(line, 0)));
            }

            createMarker(resource, OPENL_MODEL_PROBLEM_MARKER, attributes);

        } catch (Exception e) {
            handleException(e);
        }
    }

    public static void addMarkers(IResource resource, CompositeSyntaxNodeException sex, String openl) {
        SyntaxNodeException[] errors = sex.getErrors();

        for (int i = 0; i < errors.length; i++) {
            // TODO severity
            addMarker(resource, errors[i], openl, IMarker.SEVERITY_ERROR);
        }
    }

    static public int atoi(String a, int defaultValue) {
        try {
            return Integer.parseInt(a);
        } catch (Exception e) {
            return defaultValue;
        }
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

        return new CoreException(new MultiStatus(OpenlBasePlugin.PLUGIN_ID, IStatus.ERROR, "Plugin Exception:", t));
    }

    protected static IMarker createMarker(IResource resource, String type, Map<String, Object> attributes) throws Exception {
        IMarker m = resource.createMarker(type);
        m.setAttributes(attributes);
        return m;
    }

    /**
     * Generic exception handler.
     */
    public static CoreException handleException(Throwable t) {
        CoreException ce = coreException(t);
        Log.error(ce.getStatus());
        return ce;
    }

    public static void removeAllOpenlMarkers(IResource resource) {
        boolean includeSubtypes = true;
        int depth = IResource.DEPTH_INFINITE;
        try {
            resource.deleteMarkers(IOpenlModelConstants.OPENL_MODEL_PROBLEM_MARKER, includeSubtypes, depth);
        } catch (Exception e) {
            handleException(e);
        }
    }

    public static void removeAllOpenlMarkers(IResource[] resources) {
        for (int i = 0; i < resources.length; i++) {
            removeAllOpenlMarkers(resources[i]);
        }
    }

    static private String toSingleLine(String s) {
        if (s == null) {
            return "";
        }

        s = StringTool.replace(s, "\r\n", ". ");
        s = StringTool.replace(s, "\n", ". ");
        s = StringTool.replace(s, "\r", ". ");
        // s = s.replace('\r', ' ').replace('\n', '$');

        int MAX = 300;
        if (s.length() > MAX) {
            s = s.substring(0, MAX);
        }

        return s;
    }

    /**
     * Returns new CoreException with a given parameters.
     */
    // public CoreException coreException(String message, Throwable t, int code,
    // int severity) {
    // return new CoreException(new Status(severity, getLogPlugin()
    // .getDescriptor().getUniqueIdentifier(), code, getMessage(t), t));
    // }
    /**
     * Returns new CoreException with a given parameters.
     */
    static public Throwable unwrapInvocationTargetException(InvocationTargetException t) {
        Throwable cause;

        while ((cause = t.getCause()) instanceof InvocationTargetException) {
            t = (InvocationTargetException) cause;
        }

        return cause != null ? cause : t;
    }

}