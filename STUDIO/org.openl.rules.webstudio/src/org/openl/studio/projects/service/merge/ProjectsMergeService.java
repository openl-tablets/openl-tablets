package org.openl.studio.projects.service.merge;

import java.io.IOException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.projects.model.merge.CheckMergeResult;
import org.openl.studio.projects.model.merge.MergeOpMode;
import org.openl.studio.projects.model.merge.MergeResult;

public interface ProjectsMergeService {

    /**
     * Checks merge status between source and target branches of the project.
     *
     * @param project     project
     * @param otherBranch target branch name
     * @param mode        merge operation mode
     * @return merge status result
     * @throws IOException in case of I/O error
     */
    @NotNull
    CheckMergeResult checkMerge(@NotNull RulesProject project,
                                @NotBlank String otherBranch,
                                @NotNull MergeOpMode mode) throws IOException;

    /**
     * Merges branches of the project according to the specified merge operation mode.
     *
     * @param project     project
     * @param otherBranch target branch name
     * @param mode        merge operation mode
     * @return merge result
     * @throws IOException in case of I/O error
     */
    @NotNull
    MergeResult merge(@NotNull RulesProject project,
                      @NotBlank String otherBranch,
                      @NotNull MergeOpMode mode) throws IOException;

}
