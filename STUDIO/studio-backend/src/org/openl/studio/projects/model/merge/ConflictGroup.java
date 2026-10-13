package org.openl.studio.projects.model.merge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import org.openl.util.FileTypeHelper;

/**
 * Represents a group of conflicted files within a single project.
 * Files are automatically sorted with Excel files appearing first, as they have more meaning for business logic.
 */
@Schema(description = "Group of conflicted files within a single project")
public class ConflictGroup {

    /**
     * Workbooks first, as they have more meaning for business, then every other file; each by path, ignoring case.
     */
    public static final Comparator<String> FILE_ORDER = Comparator
            .comparing((String file) -> !FileTypeHelper.isExcelFile(file))
            .thenComparing(String.CASE_INSENSITIVE_ORDER);

    @Getter
    @Schema(description = "Name of the project containing conflicted files")
    private final String projectName;

    @Getter
    @Schema(description = "Path to the project in the repository")
    private final String projectPath;

    @Schema(description = "Set of conflicted file paths, sorted with Excel files first")
    private final TreeSet<String> files;

    public ConflictGroup(String projectName, String projectPath) {
        this.projectName = projectName;
        this.projectPath = projectPath;

        files = new TreeSet<>(FILE_ORDER);
    }

    public List<String> getFiles() {
        return Collections.unmodifiableList(new ArrayList<>(files));
    }

    public void addFile(String file) {
        files.add(file);
    }
}
