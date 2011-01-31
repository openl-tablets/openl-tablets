package org.openl.rules.ui;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.util.Log;
import org.openl.util.file.FileStorage;

/**
 * @author Andrei Astrouski
 */
public class FileBasedProjectHistoryManager implements ProjectHistoryManager {

    private ProjectModel projectModel;
    private FileStorage storage;

    public FileBasedProjectHistoryManager(ProjectModel projectModel, String storagePath) {
        if (projectModel == null) {
            throw new IllegalArgumentException();
        }
        if (storagePath == null) {
            throw new IllegalArgumentException();
        }
        this.projectModel = projectModel;
        storage = new FileStorage(storagePath, true);
    }

    public void save(String tableUri) {
        File sourceFile = projectModel.getSource(tableUri);
        storage.add(sourceFile);
    }

    public long[] getVersions() {
        Collection<File> files = storage.list();
        long[] versions = new long[files.size()];
        int i = 0;
        for (File file : files) {
            versions[i] = file.lastModified();
            i++;
        }
        return versions;
    }

    public boolean revert(long date) {
        File fileToRevert = getFileByDate(date);
        if (fileToRevert != null) {
            File currentSourceFile = projectModel.getSourceByName(fileToRevert.getName());
            try {
                FileUtils.copyFile(fileToRevert, currentSourceFile);
                projectModel.reset(ReloadType.FORCED);
                projectModel.buildProjectTree();
                Log.info("Project was reverted successfully");
            } catch (Exception e) {
                Log.error("Can't revert project at "
                        + new SimpleDateFormat().format(new Date(date)));
            }
            return true;
        }
        return false;
    }

    private File getFileByDate(long date) {
        List<File> files = new ArrayList<File>(
                storage.list(new AgeFileFilter(date)));
        if (!files.isEmpty()) {
            return files.get(files.size() - 1);
        }
        return null;
    }

}
