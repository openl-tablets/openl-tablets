package org.openl.rules.project.dependencies;

import java.io.File;
import java.net.MalformedURLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.conf.IUserContext;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.source.loaders.ModuleLoader;

/**
 * Dependency module loader with support of {@link RulesProjectResolver}
 * 
 * @author DLiauchuk
 *
 */
public class RulesProjectModuleLoader extends ModuleLoader {
    
    private static final Log LOG = LogFactory.getLog(RulesProjectModuleLoader.class);
    
    private RulesProjectResolver projectResolver;
        
    public RulesProjectModuleLoader(IUserContext userContext, String openlName, RulesProjectResolver projectResolver) {
        super(userContext, openlName);
        this.projectResolver = projectResolver;
    }

    @Override
    public IOpenSourceCodeModule find(String dependency, String rootFileUrl) {
        IOpenSourceCodeModule dependencySource = null;
        for (ProjectDescriptor project : projectResolver.listOpenLProjects()) {
            if (project.getName().equals(dependency)) {     
                // get path to root excel file 
                //
                String path = project.getModules().get(0).getRulesRootPath().getPath();
                try {
                    dependencySource = new URLSourceCodeModule(new File(path).toURI().toURL());
                } catch (MalformedURLException e) {
                    // now, just logging
                    LOG.error(e);
                }
            }
        }        
        return dependencySource;
    }
    

}
