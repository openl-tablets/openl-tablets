package org.openl.rules.webstudio.configuration;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.openl.config.ConfigurationManager;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.impl.local.LocalArtefactAPI;
import org.openl.rules.project.impl.local.LocalFolderAPI;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author nsamatov.
 */
@Configuration
public class DemoInitializer {
    private static final String DEMO_INITIALIZED = "demo.initialized";

    private final Logger log = LoggerFactory.getLogger(DemoInitializer.class);

    @Autowired
    private ConfigurationManager systemConfigManager;

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        ConfigurationManager appConfig = new ConfigurationManager(false, System.getProperty("webapp.root") + "/WEB-INF/conf/config.properties");
        if (!appConfig.getBooleanProperty(DEMO_INITIALIZED)) {
            log.info("Initializing Demo...");

            File userWorkspaceFolder = new File(systemConfigManager.getStringProperty("user.workspace.home"));
            LocalWorkspaceImpl userWorkspace = new LocalWorkspaceImpl(null, userWorkspaceFolder, null, null);

            for (File projectFolder : userWorkspaceFolder.listFiles((FileFilter) DirectoryFileFilter.INSTANCE)) {
                resetModifiedStatus(new LocalFolderAPI(projectFolder, new ArtefactPathImpl(projectFolder.getName()), userWorkspace));
            }

            appConfig.setProperty(DEMO_INITIALIZED, true);
            appConfig.save();

            log.info("Demo was initialized.");
        }
    }

    private void resetModifiedStatus(LocalArtefactAPI artefact) {
        long lastModified = artefact.getCreationDate();
        if (artefact.getSource().setLastModified(lastModified)) {
            if (log.isInfoEnabled()) {
                String path = artefact.getArtefactPath().getStringValue();
                String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(lastModified));
                log.info("Last modified time of '{}' is updated to '{}'", path, date);
            }
        } else {
            log.warn("Can't change last modified time for file '{}'", artefact.getSource().getAbsolutePath());
        }

        if (artefact instanceof LocalFolderAPI) {
            for (LocalArtefactAPI child : ((LocalFolderAPI) artefact).getArtefacts()) {
                resetModifiedStatus(child);
            }
        }
    }
}
