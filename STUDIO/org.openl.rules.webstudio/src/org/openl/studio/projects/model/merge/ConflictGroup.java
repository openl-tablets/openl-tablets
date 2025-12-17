package org.openl.studio.projects.model.merge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.util.FileTypeHelper;

/**
 * Represents a group of conflicted files within a single project.
 * Files are automatically sorted with Excel files appearing first, as they have more meaning for business logic.
 */
@Schema(description = "Group of conflicted files within a single project")
public class ConflictGroup {
    @Schema(description = "Name of the project containing conflicted files")
    private final String projectName;

    @Schema(description = "Path to the project in the repository")
    private final String projectPath;

    @Schema(description = "Set of conflicted file paths, sorted with Excel files first")
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
