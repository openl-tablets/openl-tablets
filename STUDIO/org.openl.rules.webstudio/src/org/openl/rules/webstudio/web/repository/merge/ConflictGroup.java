package org.openl.rules.webstudio.web.repository.merge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import org.openl.util.FileTypeHelper;

public class ConflictGroup {
    private final String projectName;
    private final String projectPath;
    private final TreeSet<String> files;

    public ConflictGroup(String projectName, String projectPath) {
        this.projectName = projectName;
        this.projectPath = projectPath;

        files = new TreeSet<>((f1, f2) -> {
            boolean isExcel1 = FileTypeHelper.isExcelFile(f1);
            boolean isExcel2 = FileTypeHelper.isExcelFile(f2);
            if (isExcel1 && isExcel2) {
                // Compare xls files
                return f1.compareToIgnoreCase(f2);
            }

            // Put xls files before other files because they have more meaning for business.
            if (isExcel1) {
                return -1;
            }

            if (isExcel2) {
                return 1;
            }

            // Compare other files
            return f1.compareToIgnoreCase(f2);
        });
    }

    public String getProjectName() {
        return projectName;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public List<String> getFiles() {
        return Collections.unmodifiableList(new ArrayList<>(files));
    }

    public void addFile(String file) {
        files.add(file);
    }
}
