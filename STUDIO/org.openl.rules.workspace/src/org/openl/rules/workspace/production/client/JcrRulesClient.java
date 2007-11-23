package org.openl.rules.workspace.production.client;

import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.RFile;
import org.openl.rules.repository.RProject;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.util.IOUtil;
import org.openl.rules.workspace.util.ZipUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class JcrRulesClient {
    private static final String ZIPPED_PROJECT_FILENAME = "__deployement.archive.zip";

    /**
     * Unpacks deployed project with given deploy id to <code>destFolder</code>.
     *
     * @param deployID identifier of deployed project
     * @param destFolder the folder to unpack the project to.
     */
    public void fetchProject(DeployID deployID, File destFolder) throws Exception {
        destFolder.mkdirs();
        FolderHelper.clearFolder(destFolder);

        RProject rProject = ProductionRepositoryFactoryProxy.getRepositoryInstance().getProject(deployID.getName());
        RFile rFile = rProject.getRootFolder().getFiles().get(0);

        File projectZip = new File(destFolder, ZIPPED_PROJECT_FILENAME);
        IOUtil.copy(rFile.getContent(), new BufferedOutputStream(new FileOutputStream(projectZip)));

        ZipUtil.unzip(projectZip, destFolder);
        projectZip.delete();
    }

}
