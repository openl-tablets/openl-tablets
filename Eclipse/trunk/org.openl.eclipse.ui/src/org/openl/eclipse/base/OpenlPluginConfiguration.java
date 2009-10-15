/*
 * Created on Jul 1, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.base;

import java.util.HashMap;
import java.util.Map;

import org.openl.eclipse.util.Debug;
import org.openl.eclipse.util.UtilBase;
import org.openl.main.OpenlMain;

/**
 * Responsibilities: - Editor/Viewer tools factories. - Document factories. -
 *
 * Scope: - workspace, global - project
 *
 * @author sam
 *
 */
public class OpenlPluginConfiguration extends UtilBase {
    // public IDocumentProvider getDocumentProvider(ITextEditor editor,
    // IResource resource) {
    // // TODO re-think: getDocumentProvider() now is Java
    // return JavaPlugin.getDefault().getCompilationUnitDocumentProvider();
    // }

    // public SourceViewerConfiguration getSourceViewerConfiguration(
    // ITextEditor editor, IResource resource) {
    // // TODO re-think: getSourceViewerConfiguration() now is Java
    // JavaTextTools tools = JavaPlugin.getDefault().getJavaTextTools();
    // return new OpenlViewerConfiguration(tools, editor);
    // }

    // public String[] getConfiguredOpenlNames(String fileURL) {
    // XxY m = getOpenlIdeExtensionManager().getFileOpenlMapping();
    //
    // Set openls = new HashSet();
    // for (Iterator iter = m.X().iterator(); iter.hasNext();) {
    // String extension = (String) iter.next();
    // if (fileURL.endsWith("." + extension)) {
    // openls.addAll(m.x_Y().f(extension));
    // }
    // }
    //
    // return (String[]) openls.toArray(new String[0]);
    // }

    static Map<Object, Object> userContextCache = new HashMap<Object, Object>();

    // public String getOpenlName(String fileURL) {
    // String[] openls = getConfiguredOpenlNames(fileURL);
    // switch (openls.length) {
    // default:
    // // TODO multiple configured openl names
    // case 1:
    // return openls[0];
    //
    // case 0:
    // return getDefaultOpenlName(fileURL);
    // }
    // }

    // ClassLoader getProjectClassLoader(IEProject project, IPluginDescriptor[]
    // pds) {
    //
    // return new
    // URLClassLoader(UrlUtil.toUrl(project.getProjectClasspathEntries()));
    //
    // }

    static public void reset() {
        userContextCache = new HashMap<Object, Object>();

        if (Debug.DEBUG) {
            Debug.debug("OpenlPluginConfiguration.clearCache()");
        }
    }

    public String getDefaultOpenlName(String fileURL) {
        return OpenlMain.getOpenlName(fileURL);
    }

    // public synchronized IUserContext getUserContext(IEProject project)
    // throws Exception {
    // IUserContext context = (IUserContext) userContextCache.get(project
    // .getProject());
    //
    // if (context == null) {
    // // Properties properties =
    // // getOpenlIdeExtensionManager().getAllOpenlExtensionsProperties();
    // Properties properties = new Properties();
    //
    // ClassLoader cl = getProjectClassLoader(project,
    // getRequiredOpenlPlugins());
    //
    // context = new EclipseProjectContext(project, cl, properties);
    //
    // userContextCache.put(project.getProject(), context);
    // }
    //
    // return context;
    // }

    // public OpenL getOpenl(IEResource resource) throws Exception {
    // String url = resource.getCanonicalURL();
    // String openlName = getOpenlName(url);
    //
    // IUserContext context = getUserContext(resource.getEProject());
    //
    // OpenL openl = OpenL.getInstance(openlName, context);
    //
    // if (Debug.DEBUG) {
    // String ID = "0x"
    // + Integer.toHexString(System.identityHashCode(openl));
    // Debug.debug("Resource=" + resource + ", openl=" + openl + ", ID="
    // + ID);
    // }
    //
    // return openl;
    // }

}