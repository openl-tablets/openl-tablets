package org.openl.rules.project.dependencies;

import org.openl.conf.IUserContext;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.source.DependencyManager;
import org.openl.syntax.code.DependencyType;

public class RulesProjectDependencyManager extends DependencyManager {
    
    private RulesProjectResolver projectResolver;
    
    public RulesProjectDependencyManager(IUserContext userContext, String openlName) {
        super(userContext, openlName);
    }

    public RulesProjectDependencyManager() {
        super();
    }

    protected void initLoaders() {        
        projectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        getDependencyLoaders().put(DependencyType.MODULE, new RulesProjectModuleLoader(getUserContext(), getOpenlName(), projectResolver));        
    }

}
