package org.openl.rules.webstudio.web.repository;

import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.workspace.uw.UserWorkspace;

public class ExcelFileProjectCreator extends AProjectCreator {
    
    private static final Log LOG = LogFactory.getLog(ExcelFileProjectCreator.class);
    
    private InputStream rulesSource;
    
    private String rulesSourceName;
    
    public ExcelFileProjectCreator(String projectName, UserWorkspace userWorkspace,
            InputStream rulesSource, String rulesSourceName) {
        super(projectName, userWorkspace);
        this.rulesSource = rulesSource;
        this.rulesSourceName = rulesSourceName;
    }

    @Override
    public String createRulesProject() {
        String errorMessage = null;
        RulesProjectBuilder projectBuilder = null;
        try {

            projectBuilder = new RulesProjectBuilder(getUserWorkspace(), getProjectName(), null);

            projectBuilder.addFile(rulesSourceName, rulesSource);

            projectBuilder.checkIn();
        } catch (Exception e) {
            if (projectBuilder != null) {
                projectBuilder.cancel();
            }
            LOG.error("Error creating project.", e);
            errorMessage = e.getMessage();
        }
        return errorMessage;
    }

}
