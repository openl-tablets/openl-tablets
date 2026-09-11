package org.openl.studio.compare.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.compare.model.ComparisonSideRequest;
import org.openl.util.FileTypeHelper;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringUtils;

/**
 * The Excel files of a project as a comparison reads them.
 *
 * <p>A file is read either from the working copy - the project as its own user has it now - or from a
 * revision the repository holds, on the branch that revision belongs to.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectComparisonService {

    private static final char SEPARATOR = '/';

    @Lookup
    public UserWorkspace getUserWorkspace() {
        return null;
    }

    /**
     * The Excel files that can be compared, by their path inside the project.
     *
     * @param project  the project the files belong to
     * @param branch   the branch to read them on, or nothing for the working copy
     * @param revision the revision to read them at, or nothing for the working copy
     * @return the paths, in alphabetical order
     */
    public List<String> excelFiles(RulesProject project, @Nullable String branch, @Nullable String revision) {
        var side = sideOf(project, branch, revision);
        return excelArtefacts(side).stream()
                .map(artefact -> insideProject(side, artefact))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /**
     * Reads the file one side of a comparison names.
     *
     * @param project the project the file belongs to
     * @param side    which file, and where it is read from
     * @return the content, under the name its format is read by
     * @throws BadRequestException when the file named is not an Excel file
     * @throws NotFoundException   when the project holds no such file there
     */
    public ComparisonContent read(RulesProject project, ComparisonSideRequest side) {
        if (!FileTypeHelper.isExcelFile(side.path())) {
            throw new BadRequestException("compare.file.not-excel.message");
        }
        var read = sideOf(project, side.branch(), side.revision());
        var artefact = excelArtefacts(read).stream()
                .filter(candidate -> insideProject(read, candidate).equals(side.path()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("file.not.found.message"));
        try {
            return new ComparisonContent(artefact.getName(), ((AProjectResource) artefact).getContent());
        } catch (ProjectException e) {
            // The repository listed the file and then could not open it; the caller is told which one.
            throw RuntimeExceptionWrapper.wrap("Cannot read '" + side.path() + "'", e);
        }
    }

    /**
     * The project one side of a comparison reads: the working copy, or what the repository holds at a
     * revision.
     *
     * <p>A revision is read by the folder the repository keeps the project in rather than by its name,
     * because the name a user knows it by can be one a rename has not published yet.
     *
     * @throws NotFoundException when the repository has no such revision of the project
     */
    private AProject sideOf(RulesProject project, @Nullable String branch, @Nullable String revision) {
        if (StringUtils.isBlank(branch) && StringUtils.isBlank(revision)) {
            return project;
        }
        try {
            return getUserWorkspace().getDesignTimeRepository()
                    .getProjectByPath(project.getDesignRepository().getId(),
                            StringUtils.trimToNull(branch),
                            project.getRealPath(),
                            StringUtils.trimToNull(revision));
        } catch (Exception e) {
            // Whatever stopped the read - the revision is gone, the repository refused it - the user is
            // told the same thing, so what it really was is left in the log.
            log.warn("Cannot read revision '{}' of '{}' on branch '{}'", revision, project.getName(), branch, e);
            throw new NotFoundException("file.version.not.found.message");
        }
    }

    /** Every Excel file of the project, wherever in it it lies. */
    private static List<AProjectArtefact> excelArtefacts(AProject project) {
        var excelArtefacts = new ArrayList<AProjectArtefact>();
        collectExcelArtefacts(project.getArtefacts(), excelArtefacts);
        return excelArtefacts;
    }

    private static void collectExcelArtefacts(Collection<? extends AProjectArtefact> artefacts,
                                              List<AProjectArtefact> excelArtefacts) {
        for (AProjectArtefact artefact : artefacts) {
            if (artefact instanceof AProjectFolder folder) {
                collectExcelArtefacts(folder.getArtefacts(), excelArtefacts);
            } else if (FileTypeHelper.isExcelFile(artefact.getName())) {
                excelArtefacts.add(artefact);
            }
        }
    }

    /**
     * The path of a file as the project knows it.
     *
     * <p>An artefact carries the path of the project before its own: the name the workspace knows the
     * project by, or the folder the repository keeps it in when a revision of it is read.
     */
    private static String insideProject(AProject project, AProjectArtefact artefact) {
        var prefix = stripLeadingSlash(project.getArtefactPath().getStringValue());
        var path = stripLeadingSlash(artefact.getArtefactPath().getStringValue());
        return path.startsWith(prefix + SEPARATOR) ? path.substring(prefix.length() + 1) : path;
    }

    private static String stripLeadingSlash(String path) {
        return path.startsWith(String.valueOf(SEPARATOR)) ? path.substring(1) : path;
    }
}
