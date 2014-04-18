package org.openl.rules.webstudio.configuration;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.PostConstruct;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.config.ConfigurationManager;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.impl.local.LocalArtefactAPI;
import org.openl.rules.project.impl.local.LocalFolderAPI;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * @author nsamatov.
 */
@Configuration
public class DemoInitializer {
    private static final String DEMO_INITIALIZED = "demo.initialized";

    private final Log log = LogFactory.getLog(DemoInitializer.class);

    @Autowired
    private ConfigurationManager systemConfigManager;

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        ConfigurationManager appConfig = new ConfigurationManager(false, System.getProperty("webapp.root") + "/WEB-INF/conf/config.properties");
        if (!appConfig.getBooleanProperty(DEMO_INITIALIZED)) {
            if (log.isInfoEnabled()) {
                log.info("Initializing Demo...");
            }

            File userWorkspaceFolder = new File(systemConfigManager.getStringProperty("user.workspace.home"));
            LocalWorkspaceImpl userWorkspace = new LocalWorkspaceImpl(null, userWorkspaceFolder, null, null);

            for (File projectFolder : userWorkspaceFolder.listFiles((FileFilter) DirectoryFileFilter.INSTANCE)) {
                resetModifiedStatus(new LocalFolderAPI(projectFolder, new ArtefactPathImpl(projectFolder.getName()), userWorkspace));
            }

            appConfig.setProperty(DEMO_INITIALIZED, true);
            appConfig.save();

            if (log.isInfoEnabled()) {
                log.info("Demo was initialized.");
            }
        }
    }

    private void resetModifiedStatus(LocalArtefactAPI artefact) {
        long lastModified = artefact.getCreationDate();
        if (artefact.getSource().setLastModified(lastModified)) {
            if (log.isInfoEnabled()) {
                String path = artefact.getArtefactPath().getStringValue();
                String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(lastModified));
                log.info(String.format("Last modified time of '%s' is updated to '%s'", path, date));
            }
        } else {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Can't change last modified time for file '%s'", artefact.getSource().getAbsolutePath()));
            }
        }

        if (artefact instanceof LocalFolderAPI) {
            for (LocalArtefactAPI child : ((LocalFolderAPI) artefact).getArtefacts()) {
                resetModifiedStatus(child);
            }
        }
    }
}
