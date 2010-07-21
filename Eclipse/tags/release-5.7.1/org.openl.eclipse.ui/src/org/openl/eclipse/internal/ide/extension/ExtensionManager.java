/*
 * Created on Oct 8, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.internal.ide.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IFileEditorMapping;
import org.openl.eclipse.ide.extension.IOpenlBuilderExtension;
import org.openl.eclipse.ide.extension.IOpenlExtensionManager;
import org.openl.eclipse.ide.extension.IOpenlLanguageExtension;
import org.openl.eclipse.util.UtilBase;
import org.openl.eclipse.util.XxY;
import org.openl.util.AOpenIterator;
import org.openl.util.IConvertor;

/**
 *
 * @author sam
 */
public class ExtensionManager extends UtilBase implements IOpenlExtensionManager {

    protected Map pluginDescriptors = new HashMap();

    public ExtensionManager() {
        register();
    }

    public Properties getAllOpenlExtensionsProperties() {
        Properties result = new Properties();

        IOpenlLanguageExtension[] langs = getOpenlLanguageExtensions();
        for (int i = 0; i < langs.length; i++) {
            result.putAll(langs[i].getProperties());
        }

        IOpenlBuilderExtension[] builderExts = getOpenlBuilderExtensions();
        for (int i = 0; i < builderExts.length; i++) {
            result.putAll(builderExts[i].getProperties());
        }

        return result;
    }

    XxY getEclipseFileEditorMapping() {
        XxY m = new XxY();

        IFileEditorMapping[] mappings = getEditorRegistry().getFileEditorMappings();
        for (int i = 0; i < mappings.length; i++) {
            String file = mappings[i].getExtension();
            IEditorDescriptor[] editorDescriptors = mappings[i].getEditors();
            for (int j = 0; j < editorDescriptors.length; j++) {
                String editor = editorDescriptors[j].getId();
                m.add(file, editor);
            }
        }
        return m;
    }

    IExtensionRegistry getExtensionRegistry() {
        return Platform.getExtensionRegistry();
    }

    /**
     * Iterates extensions for a given extensionPointId and pass extensions'
     * IConfigurationElement to the given collector. Returns collected
     * extensions.
     */
    Collection getExtensions(String extensionPointId, IConvertor collector) {
        Collection result = new ArrayList();

        IExtensionPoint point;
        IExtension[] extensions;
        IConfigurationElement[] elements;

        point = getExtensionRegistry().getExtensionPoint(extensionPointId);

        if (point != null) {
            extensions = point.getExtensions();
            for (int i = 0; i < extensions.length; i++) {
                elements = extensions[i].getConfigurationElements();
                for (int j = 0; j < elements.length; j++) {
                    result.add(elements[j]);
                }
            }
        }

        return AOpenIterator.collect(result.iterator(), collector).asList();
    }

    // IEPluginDescriptor getPluginDescriptor(IConfigurationElement element)
    // {
    // IPluginDescriptor pd =
    // element.getDeclaringExtension().getDeclaringPluginDescriptor();
    // IEPluginDescriptor epd = (IEPluginDescriptor)pluginDescriptors.get(pd);
    //
    // if (epd == null)
    // {
    // epd = new EPluginDescriptor(pd);
    // pluginDescriptors.put(pd, epd);
    // }
    //
    // return epd;
    // }

    public XxY getFileEditorMapping() {
        XxY m = new XxY();

        IOpenlLanguageExtension[] extensions = getOpenlLanguageExtensions();
        for (int i = 0; i < extensions.length; i++) {
            m.add(extensions[i].getFileEditorMapping());
        }

        return m;
    }

    public XxY getFileOpenlMapping() {
        XxY m = new XxY();

        IOpenlLanguageExtension[] extensions = getOpenlLanguageExtensions();
        for (int i = 0; i < extensions.length; i++) {
            m.add(extensions[i].getFileOpenlMapping());
        }

        return m;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.eclipse.conf.IOpenlIdeExtensionManager#getOpenlIdeBuilderExtensions()
     */
    public IOpenlBuilderExtension[] getOpenlBuilderExtensions() {
        IConvertor collector = new IConvertor() {
            public Object convert(Object o) {
                IConfigurationElement el = (IConfigurationElement) o;
                return new OpenlBuilderExtension(el);
            }
        };

        return (IOpenlBuilderExtension[]) getExtensions(EXTENSION_POINT_OPENL_BUILDER, collector).toArray(
                NO_BUILDER_EXTENSIONS);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.eclipse.ide.ext.IOpenlIdeExtensionManager#getLanguageConfigurations()
     */
    public IOpenlLanguageExtension[] getOpenlLanguageExtensions() {
        IConvertor collector = new IConvertor() {
            public Object convert(Object o) {
                IConfigurationElement el = (IConfigurationElement) o;
                return new OpenlLanguageExtension(el);
            }
        };

        return (IOpenlLanguageExtension[]) getExtensions(EXTENSION_POINT_OPENL_LANGUAGE, collector).toArray(
                NO_LANGUAGE_EXTENSIONS);
    }

    void register() {
        registerEditors(getFileEditorMapping());
    }

    void registerEditors(XxY requiredMapping) {
        // XxY currentMapping = getEclipseFileEditorMapping();
        //
        // Collection newMapping =
        // new
        // ArrayList(Arrays.asList(getEditorRegistry().getFileEditorMappings()));
        //
        // // add new files
        // Collection newFiles = new HashSet(requiredMapping.X());
        // newFiles.removeAll(currentMapping.X());
        // for (Iterator it = newFiles.iterator(); it.hasNext();)
        // {
        // String file = (String)it.next();
        // newMapping.add(new FileEditorMapping(file));
        // }
        //
        // // add new editors
        // for (Iterator iter = newMapping.iterator(); iter.hasNext();)
        // {
        // IFileEditorMapping mapping = (IFileEditorMapping)iter.next();
        // String file = mapping.getExtension();
        //
        // Collection editors = requiredMapping.x_Y().f(file);
        // if (editors == null)
        // continue;
        //
        // for (Iterator it = editors.iterator(); it.hasNext();)
        // {
        // String editor = (String)it.next();
        // if (currentMapping.contains(file, editor))
        // continue;
        //
        // IEditorDescriptor ed = getEditorRegistry().findEditor(editor);
        // if (ed != null)
        // {
        // ((FileEditorMapping)mapping).addEditor((EditorDescriptor)ed);
        // }
        // else
        // {
        // handleException(
        // "No editor with id " + editor + " for the file " + file);
        // }
        // }
        // }
        //
        // ((EditorRegistry)getEditorRegistry()).setFileEditorMappings(
        // (FileEditorMapping[])newMapping.toArray(new FileEditorMapping[0]));
    }

}