package org.openl.source.loaders;

import java.io.File;
import java.net.URL;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.openl.conf.IUserContext;
import org.openl.message.OpenLMessagesUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;

/**
 * Load module dependency from root file home directory.
 * 
 * @author DLiauchuk
 *
 */
public class DefaultModuleLoader extends ModuleLoader {
    
    /**
     * FIXME: core should not know anything about xls.
     */
    private final String[] extensions = new String[] { "xls", "xlsm", "xlsx" }; 
    
    public DefaultModuleLoader(IUserContext userContext, String openlName) {
        super(userContext, openlName);
    }

    public IOpenSourceCodeModule find(String dependency, String rootFileUrl) {
        IOpenSourceCodeModule dependencySource = null;
        try {
            String rootFolderUrl = getUpperLevel(rootFileUrl);
            URL upperFolderUrl = new URL(rootFolderUrl);
            Collection<File> xlsFiles = FileUtils.listFiles(new File(upperFolderUrl.getFile()),
                extensions,
                true);
            for (File file : xlsFiles) {
                String fileName = file.getName().substring(0, file.getName().lastIndexOf('.'));
                if (fileName.equals(dependency)) {
                    return new URLSourceCodeModule(file.toURI().toURL());
                }
            }
        } catch (Throwable t) {
            OpenLMessagesUtils.addError(String.format("Cannot find '%s' %s", dependency, t.getMessage()));            
        }
        if (dependencySource == null) {
            OpenLMessagesUtils.addError(String.format("Cannot find '%s'", dependency));
        }
        return dependencySource;
    }
    
    private String getUpperLevel(String pathUrl) {
        return pathUrl.substring(0, pathUrl.lastIndexOf('/'));
    }
}
