package org.openl.rules.webstudio.web.repository.upload;

import java.io.InputStream;

import org.openl.rules.common.ProjectException;
import org.openl.rules.workspace.uw.UserWorkspace;

public class ExcelFileProjectCreator extends AProjectCreator {
    
    private InputStream rulesSource;
    
    private String rulesSourceName;
    
    public ExcelFileProjectCreator(String projectName, UserWorkspace userWorkspace,
            InputStream rulesSource, String rulesSourceName) {
        super(projectName, userWorkspace);
        this.rulesSource = rulesSource;
        this.rulesSourceName = rulesSourceName;
    }

    @Override
    protected RulesProjectBuilder getProjectBuilder() throws ProjectException {
        RulesProjectBuilder projectBuilder = new RulesProjectBuilder(getUserWorkspace(), getProjectName());

        projectBuilder.addFile(rulesSourceName, rulesSource);
        return projectBuilder;
    }

}
