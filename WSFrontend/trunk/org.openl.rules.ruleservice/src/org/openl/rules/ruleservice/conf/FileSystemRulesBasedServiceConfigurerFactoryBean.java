package org.openl.rules.ruleservice.conf;

import java.io.File;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;

public class FileSystemRulesBasedServiceConfigurerFactoryBean extends AbstractRulesBasedServiceConfigurerFactoryBean {

    private String folderLocationPath;
    private String moduleName;

    public String getFolderLocationPath() {
        return folderLocationPath;
    }

    public void setFolderLocationPath(String folderLocationPath) {
        if (folderLocationPath == null) {
            throw new IllegalArgumentException("folderLocationPath arg can't be null");
        }
        this.folderLocationPath = folderLocationPath;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        if (moduleName == null) {
            throw new IllegalArgumentException("moduleName arg can't be null");
        }

        this.moduleName = moduleName;
    }

    @Override
    public RulesBasedServiceConfigurer getObject() throws Exception {
        RulesBasedServiceConfigurer configurer = new RulesBasedServiceConfigurer() {
            @Override
            protected RulesInstantiationStrategy getRulesSource() {
                RulesProjectResolver projectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();
                File sourceFolder = new File(folderLocationPath);
                if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
                    throw new IllegalArgumentException(String.format("Incorrect source folder for rules based service configurer has been specified: \"%s\"",
                        folderLocationPath));
                }
                ResolvingStrategy resolvingStrategy = projectResolver.isRulesProject(sourceFolder);
                if (resolvingStrategy == null) {
                    throw new IllegalArgumentException(String.format("Incorrect source folder for rules based service configurer has been specified: \"%s\"." + " Can't resolve any OpenL project.",
                        folderLocationPath));
                }
                ProjectDescriptor descriptor = null;
                try {
                    descriptor = resolvingStrategy.resolveProject(sourceFolder);
                } catch (ProjectResolvingException e) {
                    throw new OpenlNotCheckedException(e);
                }
                return getRulesInstantiationStrategy(moduleName, descriptor.getModules());
            }
        };
        return configurer;
    }

}
