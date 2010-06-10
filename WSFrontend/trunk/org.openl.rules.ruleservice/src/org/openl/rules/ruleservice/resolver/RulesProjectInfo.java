package org.openl.rules.ruleservice.resolver;

import java.io.File;
import java.util.List;

/**
 * Holds information about rules project and its modules. Information includes
 * data required to instantiate an OpenL rules as wrapper class or interface or
 * through API.
 */
public class RulesProjectInfo {
    private File project;
    private File projectBin;
    private List<RulesModuleInfo> rulesModules;

    /**
     * Constructor. Some parameters may be <code>null</code>.
     * 
     * @param project the directory where an OpenL project was downloaded to
     */
    public RulesProjectInfo(File project, File projectBin, List<RulesModuleInfo> rulesModules) {
        this.project = project;
        this.projectBin = projectBin;
        this.rulesModules = rulesModules;
    }

    public File getProject() {
        return project;
    }

    public File getProjectBin() {
        return projectBin;
    }

    public List<RulesModuleInfo> getRulesModules() {
        return rulesModules;
    }
}
