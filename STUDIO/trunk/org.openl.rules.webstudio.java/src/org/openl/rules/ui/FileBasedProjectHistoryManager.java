package org.openl.rules.ui;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.source.SourceHistoryManager;
import org.openl.util.Log;
import org.openl.util.file.FileStorage;

/**
 * @author Andrei Astrouski
 */
public class FileBasedProjectHistoryManager implements SourceHistoryManager<File> {

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

    public void save(File source) {
        storage.add(source);
    }

    public File get(long date) {
        List<File> files = new ArrayList<File>(
                storage.list(new AgeFileFilter(date)));
        if (!files.isEmpty()) {
            return files.get(files.size() - 1);
        }
        return null;
    }

    public Map<Long, File> getAll() {
        Collection<File> files = storage.list();
        Map<Long, File> versions = new TreeMap<Long, File>();
        for (File file : files) {
            versions.put(file.lastModified(), file);
        }
        return versions;
    }

    public boolean revert(long date) {
        File fileToRevert = get(date);
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

}
